package com.example.musicrecommendation.web;

import com.example.musicrecommendation.event.MatchingEvent;
import com.example.musicrecommendation.service.ChatRoomService;
import com.example.musicrecommendation.service.MatchingQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class EnhancedWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(EnhancedWebSocketController.class);

    private final ChatRoomService chatRoomService;
    private final MatchingQueueService matchingQueueService;
    private final SimpMessagingTemplate messagingTemplate;

    public EnhancedWebSocketController(ChatRoomService chatRoomService, 
                                      MatchingQueueService matchingQueueService,
                                      SimpMessagingTemplate messagingTemplate) {
        this.chatRoomService = chatRoomService;
        this.matchingQueueService = matchingQueueService;
        this.messagingTemplate = messagingTemplate;
    }

    /** 채팅 메시지 전송 */
    @MessageMapping("/send-chat-message")
    @SendToUser("/queue/chat-response")
    public Object handleChatMessage(@Payload Map<String, Object> messageData, Principal principal) {
        try {
            String roomId = (String) messageData.get("roomId");
            String content = (String) messageData.get("content");
            String messageType = (String) messageData.getOrDefault("type", "TEXT");
            Long senderId = extractUserIdFromPrincipal(principal);

            log.info("[WS] recv /send-chat-message roomId={}, contentLen={}",
                    roomId, content != null ? content.length() : -1);

            if (roomId == null || content == null) {
                return new Object() {
                    public final boolean success = false;
                    public final String error = "roomId와 content는 필수입니다";
                    public final String timestamp = LocalDateTime.now().toString();
                };
            }

            Object result = chatRoomService.sendMessage(roomId, senderId, content, messageType);

            return new Object() {
                public final boolean success = true;
                public final String message = "메시지가 전송되었습니다";
                public final Object messageResult = result;
                public final String timestamp = LocalDateTime.now().toString();
            };

        } catch (Exception e) {
            log.error("handleChatMessage error", e);
            return new Object() {
                public final boolean success = false;
                public final String error = "메시지 전송 중 오류: " + e.getMessage();
                public final String timestamp = LocalDateTime.now().toString();
            };
        }
    }

    /** 매칭 요청 처리 */
    @MessageMapping("/request-matching")
    @SendToUser("/queue/matching-result")
    public Object handleMatchingRequest(@Payload Map<String, Object> requestData, Principal principal) {
        try {
            Long userId = null;
            
            // 먼저 requestData에서 userId 추출 시도 (우선순위)
            if (requestData.containsKey("userId")) {
                try {
                    userId = Long.parseLong(requestData.get("userId").toString());
                    log.info("[WS] requestData에서 userId 추출: {}", userId);
                } catch (NumberFormatException e) {
                    log.warn("[WS] requestData userId 파싱 실패: {}", requestData.get("userId"));
                }
            }
            
            // requestData에서 추출 실패시 Principal에서 시도
            if (userId == null) {
                userId = extractUserIdFromPrincipal(principal);
                log.info("[WS] Principal에서 userId 추출: {}", userId);
            }
            
            // 그래도 null이면 오류 반환
            if (userId == null) {
                log.error("[WS] userId 추출 실패, 매칭 요청 거부");
                return new Object() {
                    public final boolean success = false;
                    public final String error = "사용자 인증이 필요합니다";
                    public final String timestamp = LocalDateTime.now().toString();
                };
            }
            
            final Long finalUserId = userId; // final 변수로 복사
            
            log.info("[WS] recv /request-matching userId={}", finalUserId);
            
            Object result = matchingQueueService.requestMatching(finalUserId);
            
            log.info("[WS] MatchingQueueService 결과: {}", result.toString());
            log.info("[WS] send /queue/matching-result userId={}", finalUserId);
            
            // Reflection을 사용해서 결과 객체의 필드 직접 확인
            boolean isSuccess = true; // 기본값
            String status = "WAITING"; // 기본적으로 WAITING 상태
            String message = "매칭 요청이 처리되었습니다";
            
            try {
                // success 필드 확인
                java.lang.reflect.Field successField = result.getClass().getDeclaredField("success");
                successField.setAccessible(true);
                isSuccess = (Boolean) successField.get(result);
                
                // status 필드 확인
                java.lang.reflect.Field statusField = result.getClass().getDeclaredField("status");
                statusField.setAccessible(true);
                String reflectionStatus = (String) statusField.get(result);
                
                // message 필드 확인
                java.lang.reflect.Field messageField = result.getClass().getDeclaredField("message");
                messageField.setAccessible(true);
                String reflectionMessage = (String) messageField.get(result);
                
                // reflection 결과 사용
                status = reflectionStatus;
                message = reflectionMessage;
                
                log.info("[WS] 최종 처리 결과: success={}, status={}, message={}", isSuccess, status, message);
                
            } catch (Exception e) {
                log.warn("[WS] Reflection 실패, 기본값 사용: {}", e.getMessage());
                // 문자열 파싱으로 fallback
                String resultString = result.toString();
                if (resultString.contains("ALREADY_MATCHED")) {
                    status = "MATCHED";
                    isSuccess = false;
                } else if (resultString.contains("ALREADY_WAITING")) {
                    status = "WAITING";
                    isSuccess = true;
                }
            }
            
            final boolean finalSuccess = isSuccess;
            final String finalStatus = status;
            final String finalMessage = message;
            
            log.info("[WS] 처리 결과: success={}, status={}, message={}", finalSuccess, finalStatus, finalMessage);
            
            // MATCHED 상태일 때도 즉시 응답 제공 (Event와 동일한 구조)
            if ("MATCHED".equals(finalStatus)) {
                log.info("[WS] 매칭 성공 - 즉시 응답과 Event 알림 모두 제공");
                return new Object() {
                    public final boolean success = true;
                    public final String message = "🎵 매칭 성공!";
                    public final String status = "MATCHED";
                    public final String type = "MATCHING_SUCCESS";
                    public final Object matchingResult = result;
                    public final Long requestUserId = finalUserId;
                    public final String timestamp = LocalDateTime.now().toString();
                };
            }
            
            // WAITING 상태만 직접 응답
            return new Object() {
                public final boolean success = finalSuccess;
                public final String message = finalMessage;
                public final Object matchingResult = result;
                public final Long requestUserId = finalUserId;
                public final String status = finalStatus;
                public final String timestamp = LocalDateTime.now().toString();
            };
            
        } catch (Exception e) {
            log.error("handleMatchingRequest error", e);
            return new Object() {
                public final boolean success = false;
                public final String error = "매칭 요청 처리 중 오류: " + e.getMessage();
                public final String timestamp = LocalDateTime.now().toString();
            };
        }
    }

    /**
     * 매칭 이벤트 리스너 - Event 기반 WebSocket 알림 처리
     */
    @EventListener
    public void handleMatchingEvent(MatchingEvent event) {
        log.info("[EVENT] 매칭 이벤트 수신: userId1={}, userId2={}, status={}", 
                event.getUserId1(), event.getUserId2(), event.getStatus());
        
        if ("SUCCESS".equals(event.getStatus())) {
            try {
                // userId1에게 WebSocket 알림 전송
                messagingTemplate.convertAndSendToUser(
                    event.getUserId1().toString(),
                    "/queue/matching-result",
                    event.getAdditionalData()
                );
                log.info("[EVENT] 매칭 성공 WebSocket 알림 전송 완료: userId1={}", event.getUserId1());
                
                // userId2에게도 WebSocket 알림 전송 (중요!)
                if (event.getUserId2() != null) {
                    messagingTemplate.convertAndSendToUser(
                        event.getUserId2().toString(),
                        "/queue/matching-result",
                        event.getAdditionalData()
                    );
                    log.info("[EVENT] 매칭 성공 WebSocket 알림 전송 완료: userId2={}", event.getUserId2());
                }
                
            } catch (Exception e) {
                log.error("[EVENT] 매칭 이벤트 WebSocket 전송 실패: userId1={}, userId2={}, error={}", 
                         event.getUserId1(), event.getUserId2(), e.getMessage());
            }
        }
    }

    /** 음악 공유 처리 */
    @MessageMapping("/share-music")
    @SendToUser("/queue/share-music-response")
    public Object handleShareMusic(@Payload Map<String, Object> musicData, Principal principal) {
        try {
            String roomId = (String) musicData.get("roomId");
            String trackName = (String) musicData.get("trackName");
            String artistName = (String) musicData.get("artistName");
            String spotifyUrl = (String) musicData.getOrDefault("spotifyUrl", "");
            Long senderId = extractUserIdFromPrincipal(principal);
            
            log.info("[WS] recv /share-music roomId={}, track={}, artist={}", 
                    roomId, trackName, artistName);
            
            if (roomId == null || trackName == null || artistName == null) {
                return new Object() {
                    public final boolean success = false;
                    public final String error = "roomId, trackName, artistName은 필수입니다";
                    public final String timestamp = LocalDateTime.now().toString();
                };
            }
            
            if (senderId == null) {
                return new Object() {
                    public final boolean success = false;
                    public final String error = "사용자 인증이 필요합니다";
                    public final String timestamp = LocalDateTime.now().toString();
                };
            }
            
            // ChatRoomService를 통해 음악 공유 (자동으로 히스토리에도 저장됨)
            Object result = chatRoomService.shareMusicTrack(roomId, senderId, trackName, artistName, spotifyUrl);
            
            log.info("[WS] 음악 공유 완료: senderId={}, track={} - {}", senderId, artistName, trackName);
            
            return new Object() {
                public final boolean success = true;
                public final String message = "음악이 공유되었습니다";
                public final Object shareResult = result;
                public final String timestamp = LocalDateTime.now().toString();
            };
            
        } catch (Exception e) {
            log.error("handleShareMusic error", e);
            return new Object() {
                public final boolean success = false;
                public final String error = "음악 공유 중 오류: " + e.getMessage();
                public final String timestamp = LocalDateTime.now().toString();
            };
        }
    }

    private Long extractUserIdFromPrincipal(Principal principal) {
        if (principal != null && principal.getName() != null) {
            try {
                Long parsedUserId = Long.parseLong(principal.getName());
                log.info("[WS] Principal 이름으로부터 userId 파싱 성공: {}", parsedUserId);
                return parsedUserId;
            } catch (NumberFormatException e) {
                log.warn("[WS] Principal 이름 파싱 실패: {}", principal.getName());
            }
        } else {
            log.warn("[WS] Principal이 null이거나 이름이 없음");
        }
        return null; // 기본값 반환하지 않음
    }
}
