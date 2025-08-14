package com.example.musicrecommendation.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1:1 채팅방 관리 서비스
 */
@Service
public class ChatRoomService {

    private final SimpMessagingTemplate messagingTemplate;

    // 채팅방 정보 저장 (roomId -> 채팅방 정보)
    private final Map<String, Object> chatRooms = new ConcurrentHashMap<>();

    // 채팅방별 메시지 히스토리 (roomId -> 메시지 리스트)
    private final Map<String, List<Object>> chatHistory = new ConcurrentHashMap<>();

    // 사용자별 현재 채팅방 (userId -> roomId)
    private final Map<Long, String> userChatRooms = new ConcurrentHashMap<>();

    public ChatRoomService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 1:1 채팅방 생성
     */
    public String createChatRoom(Long user1Id, Long user2Id) {
        // 채팅방 ID 생성 (작은 ID가 먼저 오도록)
        String roomId = "room_" + Math.min(user1Id, user2Id) + "_" + Math.max(user1Id, user2Id);

        // 채팅방 정보 생성
        Object chatRoomInfo = new Object() {
            public final String id = roomId;
            public final String name = "음악 매칭 채팅방";
            public final Long[] participants = {user1Id, user2Id};
            public final LocalDateTime createdAt = LocalDateTime.now();
            public final boolean active = true;
            public final String type = "PRIVATE_MATCH";
        };

        // 채팅방 정보 저장
        chatRooms.put(roomId, chatRoomInfo);
        chatHistory.put(roomId, new ArrayList<>());

        // 사용자별 채팅방 매핑
        userChatRooms.put(user1Id, roomId);
        userChatRooms.put(user2Id, roomId);

        // 환영 메시지 추가
        addSystemMessage(roomId, "🎵 음악 매칭 채팅방에 오신 것을 환영합니다! 서로의 음악 취향을 공유해보세요!");

        return roomId;
    }

    /**
     * 채팅 메시지 전송
     */
    public Object sendMessage(String roomId, Long senderId, String messageContent, String messageType) {
        // 채팅방 존재 확인
        if (!chatRooms.containsKey(roomId)) {
            return new Object() {
                public final boolean success = false;
                public final String error = "존재하지 않는 채팅방입니다";
            };
        }

        // 메시지 객체 생성
        Object messageObj = new Object() {
            public final String id = UUID.randomUUID().toString();
            public final String room = roomId;
            public final Long sender = senderId;
            public final String content = messageContent;
            public final String type = messageType != null ? messageType : "TEXT";
            public final LocalDateTime timestamp = LocalDateTime.now();
            public final boolean isSystemMessage = false;
        };

        // 메시지 히스토리에 추가
        chatHistory.get(roomId).add(messageObj);

        // 채팅방 참여자들에게 실시간 전송
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

    /**
     * 음악 공유 메시지 전송
     */
    public Object shareMusicTrack(String roomId, Long senderId, String trackName, String artist, String spotifyUrl) {
        Object musicMessage = new Object() {
            public final String id = UUID.randomUUID().toString();
            public final String room = roomId;
            public final Long sender = senderId;
            public final String type = "MUSIC_SHARE";
            public final Object musicData = new Object() {
                public final String track = trackName;
                public final String artistName = artist;
                public final String url = spotifyUrl;
                public final String previewText = "🎵 " + artist + " - " + trackName;
            };
            public final LocalDateTime timestamp = LocalDateTime.now();
            public final boolean isSystemMessage = false;
        };

        // 메시지 히스토리에 추가
        chatHistory.get(roomId).add(musicMessage);

        // 실시간 전송
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

    /**
     * 채팅방 메시지 히스토리 조회
     */
    public Object getChatHistory(String roomId, Long userId) {
        // 채팅방 존재 및 권한 확인
        if (!chatRooms.containsKey(roomId)) {
            return new Object() {
                public final boolean success = false;
                public final String error = "존재하지 않는 채팅방입니다";
            };
        }

        if (!userChatRooms.get(userId).equals(roomId)) {
            return new Object() {
                public final boolean success = false;
                public final String error = "채팅방 접근 권한이 없습니다";
            };
        }

        List<Object> messageList = chatHistory.getOrDefault(roomId, new ArrayList<>());

        return new Object() {
            public final boolean success = true;
            public final String room = roomId;
            public final Object roomInfo = chatRooms.get(roomId);
            public final List<Object> messageHistory = messageList;
            public final int totalMessages = messageList.size();
            public final String retrievedAt = LocalDateTime.now().toString();
        };
    }

    /**
     * 사용자의 현재 채팅방 정보 조회
     */
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

        // 최근 5개 메시지만 가져오기
        List<Object> lastMessages = recentMessages.size() > 5
                ? recentMessages.subList(recentMessages.size() - 5, recentMessages.size())
                : recentMessages;

        return new Object() {
            public final boolean hasActiveChatRoom = true;
            public final Object roomInformation = roomData;
            public final List<Object> recentMessages = lastMessages;
            public final int totalMessages = recentMessages.size();
            public final boolean canSendMessage = true;
        };
    }

    /**
     * 채팅방 비활성화
     */
    public void deactivateChatRoom(String roomId) {
        if (chatRooms.containsKey(roomId)) {
            // 시스템 메시지 추가
            addSystemMessage(roomId, "💔 매칭이 종료되었습니다. 채팅방이 비활성화됩니다.");

            // 채팅방 참여자들에게 종료 알림
            messagingTemplate.convertAndSend("/topic/chat/" + roomId, new Object() {
                public final String type = "ROOM_DEACTIVATED";
                public final String message = "채팅방이 비활성화되었습니다";
                public final String timestamp = LocalDateTime.now().toString();
            });

            // 채팅방 정보에서 사용자 매핑 제거는 하지 않음 (히스토리 보존)
        }
    }

    /**
     * 시스템 메시지 추가
     */
    private void addSystemMessage(String roomId, String content) {
        Object systemMessage = new Object() {
            public final String id = UUID.randomUUID().toString();
            public final String room = roomId;
            public final Long senderId = 0L; // 시스템 메시지
            public final String messageContent = content;
            public final String type = "SYSTEM";
            public final LocalDateTime timestamp = LocalDateTime.now();
            public final boolean isSystemMessage = true;
        };

        chatHistory.get(roomId).add(systemMessage);

        // 실시간 전송
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, new Object() {
            public final String type = "SYSTEM_MESSAGE";
            public final Object message = systemMessage;
            public final String timestamp = LocalDateTime.now().toString();
        });
    }

    /**
     * 채팅방 통계
     */
    public Object getChatRoomStats() {
        int totalRooms = chatRooms.size();
        int totalMsgCount = chatHistory.values().stream()
                .mapToInt(List::size)
                .sum();

        return new Object() {
            public final boolean success = true;
            public final String message = "채팅방 통계";
            public final Object statistics = new Object() {
                public final int totalChatRooms = totalRooms;
                public final int totalMessages = totalMsgCount;
                public final int activeChatRooms = userChatRooms.size() / 2; // 쌍의 개수
                public final double avgMessagesPerRoom = totalRooms > 0 ? (double)totalMsgCount / totalRooms : 0;
            };
            public final String timestamp = LocalDateTime.now().toString();
        };
    }

    /**
     * 채팅방 나가기
     */
    public Object leaveChatRoom(String roomId, Long userId) {
        String userRoomId = userChatRooms.get(userId);

        if (userRoomId == null || !userRoomId.equals(roomId)) {
            return new Object() {
                public final boolean success = false;
                public final String error = "해당 채팅방에 참여하고 있지 않습니다";
            };
        }

        // 사용자 매핑 제거
        userChatRooms.remove(userId);

        // 나가기 시스템 메시지 추가
        addSystemMessage(roomId, "👋 사용자가 채팅방을 나갔습니다.");

        return new Object() {
            public final boolean success = true;
            public final String message = "채팅방을 나갔습니다";
            public final String leftRoomId = roomId;
            public final LocalDateTime leftAt = LocalDateTime.now();
        };
    }
}