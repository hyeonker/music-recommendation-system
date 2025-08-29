// src/main/java/com/example/musicrecommendation/service/ChatRoomService.java
package com.example.musicrecommendation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService; // 암호화 저장

    // In-memory 저장 (실시간 브로드캐스트/데모용)
    private final Map<String, Object> chatRooms = new ConcurrentHashMap<>();
    private final Map<String, List<Object>> chatHistory = new ConcurrentHashMap<>();
    private final Map<Long, String> userChatRooms = new ConcurrentHashMap<>();

    /** 문자열 roomId에서 "마지막 숫자"를 DB roomId로 사용 (예: room_1_22 -> 22, "22" -> 22) */
    private long toDbRoomId(String roomId) {
        if (roomId == null) return 0L;
        try {
            return Long.parseLong(roomId); // 숫자만인 경우
        } catch (Exception ignore) { /* fall through */ }
        var p = java.util.regex.Pattern.compile("(\\d+)(?!.*\\d)");
        var m = p.matcher(roomId);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    /** 채팅방 생성 (매칭 성공 시 호출) */
    public String createChatRoom(Long user1Id, Long user2Id) {
        String roomId = "room_" + Math.min(user1Id, user2Id) + "_" + Math.max(user1Id, user2Id);

        Object chatRoomInfo = new Object() {
            public final String id = roomId;
            public final String name = "음악 매칭 채팅방";
            public final Long[] participants = {user1Id, user2Id};
            public final LocalDateTime createdAt = LocalDateTime.now();
            public final boolean active = true;
            public final String type = "PRIVATE_MATCH";
        };

        chatRooms.put(roomId, chatRoomInfo);
        chatHistory.put(roomId, new ArrayList<>());
        userChatRooms.put(user1Id, roomId);
        userChatRooms.put(user2Id, roomId);

        addSystemMessage(roomId, "🎵 음악 매칭 채팅방에 오신 것을 환영합니다! 서로의 음악 취향을 공유해보세요!");

        return roomId;
    }

    /** 채팅 메시지 전송 + DB 저장(암호화) + 실시간 브로드캐스트 */
    public Object sendMessage(String roomId, Long senderId, String messageContent, String messageType) {
        if (!chatRooms.containsKey(roomId)) {
            return new Object() {
                public final boolean success = false;
                public final String error = "존재하지 않는 채팅방입니다";
            };
        }

        // DB 저장
        try {
            long dbRoomId = toDbRoomId(roomId);
            if (dbRoomId > 0) {
                chatMessageService.saveText(dbRoomId, senderId, messageContent);
            }
        } catch (Exception e) {
            System.err.println("[ChatRoomService] saveText failed: " + e.getMessage());
        }

        final String t = (messageType != null && !messageType.isBlank()) ? messageType : "TEXT";

        Object messageObj = new Object() {
            public final String id = UUID.randomUUID().toString();
            public final String room = roomId;
            public final Long sender = senderId;
            public final String content = messageContent;
            public final String type = t;
            public final LocalDateTime timestamp = LocalDateTime.now();
            public final boolean isSystemMessage = false;
        };

        chatHistory.computeIfAbsent(roomId, k -> new ArrayList<>()).add(messageObj);

        // 실시간 브로드캐스트
        System.out.println("[ChatRoomService] sendMessage roomId=" + roomId + " senderId=" + senderId + " type=" + t);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, new Object() {
            public final String type = "NEW_MESSAGE";
            public final Object message = messageObj;
            public final String timestamp = LocalDateTime.now().toString();
        });

        return new Object() {
            public final boolean success = true;
            public final String message = "메시지가 전송되었습니다";
            public final Object messageInfo = messageObj;
        };
    }

    /** 음악 공유 메시지(라인 문자열) 저장 + 브로드캐스트 */
    public Object shareMusicTrack(String roomId, Long senderId, String trackName, String artist, String spotifyUrl) {
        try {
            long dbRoomId = toDbRoomId(roomId);
            if (dbRoomId > 0) {
                String line = "🎵 " + (artist == null ? "" : artist) + " - " +
                        (trackName == null ? "" : trackName) +
                        (spotifyUrl == null || spotifyUrl.isBlank() ? "" : " (" + spotifyUrl + ")");
                chatMessageService.saveText(dbRoomId, senderId, line);
            }
        } catch (Exception e) {
            System.err.println("[ChatRoomService] saveText (music) failed: " + e.getMessage());
        }

        Object musicMessage = new Object() {
            public final String id = UUID.randomUUID().toString();
            public final String room = roomId;
            public final Long sender = senderId;
            public final String type = "MUSIC_SHARE";
            public final Object musicData = new Object() {
                public final String track = trackName;
                public final String artistName = artist;
                public final String url = spotifyUrl;
                public final String previewText = "🎵 " +
                        (artist == null ? "" : artist) + " - " + (trackName == null ? "" : trackName);
            };
            public final LocalDateTime timestamp = LocalDateTime.now();
            public final boolean isSystemMessage = false;
        };

        chatHistory.computeIfAbsent(roomId, k -> new ArrayList<>()).add(musicMessage);

        messagingTemplate.convertAndSend("/topic/chat/" + roomId, new Object() {
            public final String type = "MUSIC_SHARED";
            public final Object message = musicMessage;
            public final String timestamp = LocalDateTime.now().toString();
        });

        return new Object() {
            public final boolean success = true;
            public final String message = "음악이 공유되었습니다";
            public final Object sharedMusic = musicMessage;
        };
    }

    /** 채팅방 비활성화(매칭 종료 등) */
    public void deactivateChatRoom(String roomId) {
        if (chatRooms.containsKey(roomId)) {
            addSystemMessage(roomId, "💔 매칭이 종료되었습니다. 채팅방이 비활성화됩니다.");
            messagingTemplate.convertAndSend("/topic/chat/" + roomId, new Object() {
                public final String type = "ROOM_DEACTIVATED";
                public final String message = "채팅방이 비활성화되었습니다";
                public final String timestamp = LocalDateTime.now().toString();
            });
        }
    }

    /** 채팅방 히스토리 조회 (간단한 접근 권한 체크 포함) */
    public Object getChatHistory(String roomId, Long userId) {
        if (!chatRooms.containsKey(roomId)) {
            return new Object() {
                public final boolean success = false;
                public final String error = "존재하지 않는 채팅방입니다";
            };
        }
        if (!Objects.equals(userChatRooms.get(userId), roomId)) {
            return new Object() {
                public final boolean success = false;
                public final String error = "채팅방 접근 권한이 없습니다";
            };
        }
        List<Object> list = chatHistory.getOrDefault(roomId, new ArrayList<>());

        return new Object() {
            public final boolean success = true;
            public final String room = roomId;
            public final Object roomInfo = chatRooms.get(roomId);
            public final List<Object> messageHistory = list;
            public final int totalMessages = list.size();
            public final String retrievedAt = LocalDateTime.now().toString();
        };
    }

    /** 유저의 활성 채팅방 요약 조회 */
    public Object getUserChatRoom(Long userId) {
        String roomId = userChatRooms.get(userId);
        if (roomId == null) {
            return new Object() {
                public final boolean hasActiveChatRoom = false;
                public final String message = "현재 활성화된 채팅방이 없습니다";
            };
        }
        Object roomData = chatRooms.get(roomId);
        List<Object> recentMessages = chatHistory.getOrDefault(roomId, new ArrayList<>());
        List<Object> last = recentMessages.size() > 5
                ? recentMessages.subList(recentMessages.size() - 5, recentMessages.size())
                : recentMessages;

        return new Object() {
            public final boolean hasActiveChatRoom = true;
            public final Object roomInformation = roomData;
            public final List<Object> recentMessages = last;
            public final int totalMessages = recentMessages.size();
            public final boolean canSendMessage = true;
        };
    }

    /** 전체 채팅방 통계 */
    public Object getChatRoomStats() {
        int totalRooms = chatRooms.size();
        int totalMsgCount = chatHistory.values().stream().mapToInt(List::size).sum();

        return new Object() {
            public final boolean success = true;
            public final Object statistics = new Object() {
                public final int totalChatRooms = totalRooms;
                public final int totalMessages = totalMsgCount;
                public final int activeChatRooms = userChatRooms.size() / 2;
                public final double avgMessagesPerRoom = totalRooms > 0 ? (double) totalMsgCount / totalRooms : 0;
            };
            public final String timestamp = LocalDateTime.now().toString();
        };
    }

    /** 채팅방 나가기 */
    public Object leaveChatRoom(String roomId, Long userId) {
        String userRoomId = userChatRooms.get(userId);
        if (userRoomId == null || !userRoomId.equals(roomId)) {
            return new Object() {
                public final boolean success = false;
                public final String error = "해당 채팅방에 참여하고 있지 않습니다";
            };
        }
        userChatRooms.remove(userId);
        addSystemMessage(roomId, "👋 사용자가 채팅방을 나갔습니다.");

        return new Object() {
            public final boolean success = true;
            public final String message = "채팅방을 나갔습니다";
            public final LocalDateTime leftAt = LocalDateTime.now();
        };
    }

    /** 시스템 메시지 추가 + 브로드캐스트 */
    private void addSystemMessage(String roomId, String content) {
        Object systemMessage = new Object() {
            public final String id = UUID.randomUUID().toString();
            public final String room = roomId;
            public final Long senderId = 0L;
            public final String messageContent = content;
            public final String type = "SYSTEM";
            public final LocalDateTime timestamp = LocalDateTime.now();
            public final boolean isSystemMessage = true;
        };

        chatHistory.computeIfAbsent(roomId, k -> new ArrayList<>()).add(systemMessage);

        messagingTemplate.convertAndSend("/topic/chat/" + roomId, new Object() {
            public final String type = "SYSTEM_MESSAGE";
            public final Object message = systemMessage;
            public final String timestamp = LocalDateTime.now().toString();
        });
    }
}
