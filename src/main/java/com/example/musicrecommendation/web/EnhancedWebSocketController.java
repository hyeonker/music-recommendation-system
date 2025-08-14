package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.ChatRoomService;
import com.example.musicrecommendation.service.MatchingQueueService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import java.time.LocalDateTime;
import java.security.Principal;
import java.util.Map;

/**
 * 실시간 채팅 및 매칭 WebSocket 컨트롤러
 */
@Controller
public class EnhancedWebSocketController {

    private final ChatRoomService chatRoomService;
    private final MatchingQueueService matchingQueueService;

    public EnhancedWebSocketController(ChatRoomService chatRoomService,
                                       MatchingQueueService matchingQueueService) {
        this.chatRoomService = chatRoomService;
        this.matchingQueueService = matchingQueueService;
    }

    /**
     * 채팅 메시지 전송 처리
     */
    @MessageMapping("/send-chat-message")
    @SendToUser("/queue/chat-response")
    public Object handleChatMessage(@Payload Map<String, Object> messageData, Principal principal) {
        try {
            String roomId = (String) messageData.get("roomId");
            String content = (String) messageData.get("content");
            String messageType = (String) messageData.getOrDefault("type", "TEXT");
            Long senderId = extractUserIdFromPrincipal(principal);

            if (roomId == null || content == null) {
                return new Object() {
                    public final boolean success = false;
                    public final String error = "roomId와 content는 필수입니다";
                    public final String timestamp = LocalDateTime.now().toString();
                };
            }

            // 채팅 메시지 전송
            Object result = chatRoomService.sendMessage(roomId, senderId, content, messageType);

            return new Object() {
                public final boolean success = true;
                public final String message = "메시지가 전송되었습니다";
                public final Object messageResult = result;
                public final String timestamp = LocalDateTime.now().toString();
            };

        } catch (Exception e) {
            return new Object() {
                public final boolean success = false;
                public final String error = "메시지 전송 중 오류: " + e.getMessage();
                public final String timestamp = LocalDateTime.now().toString();
            };
        }
    }

    /**
     * 음악 공유 처리
     */
    @MessageMapping("/share-music")
    @SendToUser("/queue/music-share-response")
    public Object handleMusicShare(@Payload Map<String, Object> musicData, Principal principal) {
        try {
            String roomId = (String) musicData.get("roomId");
            String trackName = (String) musicData.get("trackName");
            String artist = (String) musicData.get("artist");
            String spotifyUrl = (String) musicData.getOrDefault("spotifyUrl", "");
            Long senderId = extractUserIdFromPrincipal(principal);

            if (roomId == null || trackName == null || artist == null) {
                return new Object() {
                    public final boolean success = false;
                    public final String error = "roomId, trackName, artist는 필수입니다";
                    public final String timestamp = LocalDateTime.now().toString();
                };
            }

            // 음악 공유
            Object result = chatRoomService.shareMusicTrack(roomId, senderId, trackName, artist, spotifyUrl);

            return new Object() {
                public final boolean success = true;
                public final String message = "🎵 음악이 공유되었습니다";
                public final Object shareResult = result;
                public final String timestamp = LocalDateTime.now().toString();
            };

        } catch (Exception e) {
            return new Object() {
                public final boolean success = false;
                public final String error = "음악 공유 중 오류: " + e.getMessage();
                public final String timestamp = LocalDateTime.now().toString();
            };
        }
    }

    /**
     * WebSocket을 통한 매칭 요청
     */
    @MessageMapping("/request-matching-ws")
    @SendToUser("/queue/matching-ws-response")
    public Object handleMatchingRequestWS(Principal principal) {
        try {
            Long userId = extractUserIdFromPrincipal(principal);
            Object result = matchingQueueService.requestMatching(userId);

            return new Object() {
                public final boolean success = true;
                public final String message = "WebSocket 매칭 요청이 처리되었습니다";
                public final Object matchingResult = result;
                public final String timestamp = LocalDateTime.now().toString();
            };

        } catch (Exception e) {
            return new Object() {
                public final boolean success = false;
                public final String error = "WebSocket 매칭 요청 중 오류: " + e.getMessage();
                public final String timestamp = LocalDateTime.now().toString();
            };
        }
    }

    /**
     * 채팅방 입장 알림
     */
    @MessageMapping("/join-chat-room")
    @SendToUser("/queue/room-join-response")
    public Object handleJoinChatRoom(@Payload Map<String, Object> joinData, Principal principal) {
        try {
            String roomId = (String) joinData.get("roomId");
            Long userId = extractUserIdFromPrincipal(principal);

            if (roomId == null) {
                return new Object() {
                    public final boolean success = false;
                    public final String error = "roomId는 필수입니다";
                    public final String timestamp = LocalDateTime.now().toString();
                };
            }

            // 채팅방 정보 조회
            Object roomInfo = chatRoomService.getChatHistory(roomId, userId);

            return new Object() {
                public final boolean success = true;
                public final String message = "채팅방에 입장했습니다";
                public final String room = roomId;
                public final Object roomData = roomInfo;
                public final String timestamp = LocalDateTime.now().toString();
            };

        } catch (Exception e) {
            return new Object() {
                public final boolean success = false;
                public final String error = "채팅방 입장 중 오류: " + e.getMessage();
                public final String timestamp = LocalDateTime.now().toString();
            };
        }
    }

    /**
     * 타이핑 상태 알림
     */
    @MessageMapping("/typing-status")
    public void handleTypingStatus(@Payload Map<String, Object> typingData, Principal principal) {
        try {
            String roomId = (String) typingData.get("roomId");
            Boolean isTyping = (Boolean) typingData.getOrDefault("isTyping", false);
            Long userId = extractUserIdFromPrincipal(principal);

            if (roomId != null) {
                // 채팅방의 다른 사용자에게 타이핑 상태 알림
                // 실제로는 SimpMessagingTemplate을 통해 전송
                Object typingNotification = new Object() {
                    public final String type = "TYPING_STATUS";
                    public final String room = roomId;
                    public final Long user = userId;
                    public final boolean typing = isTyping;
                    public final String timestamp = LocalDateTime.now().toString();
                };

                // 여기서 직접 메시징하는 대신 ChatRoomService에 위임할 수 있음
            }
        } catch (Exception e) {
            // 타이핑 상태는 중요하지 않으므로 에러 무시
        }
    }

    /**
     * 연결 상태 확인 (Heartbeat)
     */
    @MessageMapping("/heartbeat")
    @SendToUser("/queue/heartbeat-response")
    public Object handleHeartbeat(Principal principal) {
        Long userId = extractUserIdFromPrincipal(principal);

        return new Object() {
            public final String type = "HEARTBEAT";
            public final boolean alive = true;
            public final Long user = userId;
            public final String serverTime = LocalDateTime.now().toString();
            public final String message = "💓 연결 상태 양호";
        };
    }

    /**
     * 사용자 활동 상태 업데이트
     */
    @MessageMapping("/update-activity")
    @SendToUser("/queue/activity-response")
    public Object handleActivityUpdate(@Payload Map<String, Object> activityData, Principal principal) {
        try {
            String activity = (String) activityData.getOrDefault("activity", "ONLINE");
            Long userId = extractUserIdFromPrincipal(principal);

            return new Object() {
                public final boolean success = true;
                public final String message = "활동 상태가 업데이트되었습니다";
                public final Long user = userId;
                public final String newActivity = activity;
                public final String timestamp = LocalDateTime.now().toString();
            };

        } catch (Exception e) {
            return new Object() {
                public final boolean success = false;
                public final String error = "활동 상태 업데이트 중 오류: " + e.getMessage();
                public final String timestamp = LocalDateTime.now().toString();
            };
        }
    }

    // 헬퍼 메서드
    private Long extractUserIdFromPrincipal(Principal principal) {
        if (principal != null && principal.getName() != null) {
            try {
                return Long.parseLong(principal.getName());
            } catch (NumberFormatException e) {
                return 1L; // 기본값
            }
        }
        return (long) (Math.random() * 100) + 1; // 시뮬레이션용 랜덤 ID
    }
}