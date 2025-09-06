package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.ChatMessageService;
import com.example.musicrecommendation.service.SecureChatRoomService;
import com.example.musicrecommendation.service.ChatRateLimitService;
import com.example.musicrecommendation.service.ChatExpiryNotificationService;
import com.example.musicrecommendation.config.ChatSecurityConfig;
import com.example.musicrecommendation.web.dto.ChatMessageCreateRequest;
import com.example.musicrecommendation.web.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Controller
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WebSocketChatController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.example.musicrecommendation.service.UserService userService;
    private final SecureChatRoomService secureChatRoomService;
    private final ChatRateLimitService rateLimitService;
    private final ChatExpiryNotificationService expiryNotificationService;
    private final ChatSecurityConfig securityConfig;

    /**
     * 특정 채팅방에 메시지 전송
     * 클라이언트에서 /app/chat.sendMessage/{roomId} 로 메시지 전송
     */
    @MessageMapping("/chat.sendMessage/{roomId}")
    public void sendMessage(@DestinationVariable String roomId,
                            @Payload ChatMessageCreateRequest message) {
        try {
            Long senderId = message.senderId() != null ? message.senderId() : 1L;
            
            // 보안 로깅
            if (securityConfig.isProductionMode()) {
                System.out.println("💬 채팅 메시지 수신 - roomId: " + securityConfig.maskRoomId(roomId) + 
                                  ", 사용자: " + securityConfig.maskUserId(senderId));
            } else {
                System.out.println("🔥 WebSocketChatController: 채팅 메시지 수신 - roomId: " + roomId + ", 내용: " + message.content());
            }
            
            // 1. Rate Limiting 검사
            if (!rateLimitService.isMessageAllowed(senderId)) {
                final String finalRoomId = roomId;
                final Object rateLimitResponse = new Object() {
                    public final String id = "-1";
                    public final String roomId = finalRoomId;
                    public final int senderId = -1;
                    public final String senderName = "시스템";
                    public final String content = "⏰ 메시지를 너무 빠르게 보내고 계시네요! 4초 후에 다시 시도해주세요. (분당 최대 15개)";
                    public final String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
                };
                messagingTemplate.convertAndSend("/topic/room." + roomId, rateLimitResponse);
                return;
            }
            
            // 2. 메시지 크기 제한 검사
            if (!rateLimitService.isMessageSizeAllowed(message.content())) {
                final String finalRoomId = roomId;
                final Object sizeLimitResponse = new Object() {
                    public final String id = "-1";
                    public final String roomId = finalRoomId;
                    public final int senderId = -1;
                    public final String senderName = "시스템";
                    public final String content = "📝 메시지가 너무 길어요! 300자 이내로 작성해주세요. 음악 이야기는 간결하게! 🎵";
                    public final String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
                };
                messagingTemplate.convertAndSend("/topic/room." + roomId, sizeLimitResponse);
                return;
            }
            
            // 보안 검증: 사용자가 해당 채팅방에 접근 권한이 있는지 확인
            if (!secureChatRoomService.hasRoomAccess(roomId, senderId)) {
                System.err.println("❌ 채팅방 접근 권한 없음 - roomId: " + roomId + ", userId: " + senderId);
                
                final String finalRoomId = roomId;
                final Object errorResponse = new Object() {
                    public final String id = "-1";
                    public final String roomId = finalRoomId;
                    public final int senderId = -1;
                    public final String senderName = "시스템";
                    public final String content = "🚫 이 채팅방에 참여할 수 없습니다. 새로운 매칭을 시작해보세요!";
                    public final String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
                };
                
                messagingTemplate.convertAndSend("/topic/room." + roomId, errorResponse);
                return;
            }
            
            // 채팅방 활동 업데이트 및 연장 확인
            boolean wasExtended = secureChatRoomService.updateRoomActivityWithExtensionCheck(roomId);
            
            // 만료 임박 상태에서 연장된 경우 알림 전송
            if (wasExtended) {
                expiryNotificationService.sendRoomExtensionNotification(roomId);
            }
            
            // roomId를 직접 DB에 저장 (hex string 형식)
            String dbRoomId = roomId;
            
            System.out.println("🔥 WebSocketChatController: roomId: " + roomId + ", senderId: " + senderId);
            
            // 메시지 저장
            var savedMessage = chatMessageService.saveText(dbRoomId, senderId, message.content());
            
            // 사용자 이름 조회
            String userName = userService.findUserById(senderId)
                .map(user -> user.getName())
                .orElse("사용자#" + senderId);
            
            System.out.println("🔥 WebSocketChatController: 사용자 이름: " + userName);
            
            // 프론트엔드 ChatMessage 인터페이스와 일치하는 응답 생성
            final String finalRoomId = roomId; // UUID 원본 사용
            final String finalUserName = userName;
            final Object response = new Object() {
                public final String id = savedMessage.id().toString();
                public final String roomId = finalRoomId;
                public final int senderId = savedMessage.senderId().intValue();
                public final String senderName = finalUserName;
                public final String content = savedMessage.content();
                public final String timestamp = OffsetDateTime.ofInstant(savedMessage.createdAt(), ZoneOffset.UTC).toString();
                // 프론트엔드 호환을 위해 createdAt도 추가
                public final String createdAt = OffsetDateTime.ofInstant(savedMessage.createdAt(), ZoneOffset.UTC).toString();
            };
            
            System.out.println("🔥 WebSocketChatController: 응답 생성 완료, 브로드캐스트 시작");
            
            // 명시적으로 채팅방 토픽에 브로드캐스트 (원본 roomId 사용)
            String broadcastTopic = "/topic/room." + roomId;
            messagingTemplate.convertAndSend(broadcastTopic, response);
            System.out.println("✅ 메시지 브로드캐스트 완료: " + broadcastTopic);
            
            // UUID roomId 사용으로 정규화 불필요
            
        } catch (Exception e) {
            System.err.println("❌ WebSocketChatController: 메시지 전송 실패 - " + e.getMessage());
            e.printStackTrace();
            
            // 에러 메시지 전송
            final String finalRoomId = roomId;
            final Object errorResponse = new Object() {
                public final String id = "-1";
                public final String roomId = finalRoomId;
                public final int senderId = -1;
                public final String senderName = "시스템";
                public final String content = "메시지 전송 실패: " + e.getMessage();
                public final String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
            };
            
            messagingTemplate.convertAndSend("/topic/room." + roomId, errorResponse);
        }
    }

    /**
     * 사용자가 채팅방에 입장했을 때
     */
    @MessageMapping("/chat.addUser/{roomId}")
    public void addUser(@DestinationVariable String roomId,
                       @Payload ChatMessageCreateRequest message) {
        try {
            Long senderId = message.senderId() != null ? message.senderId() : 1L;
            String username = message.content() != null ? message.content() : "사용자#" + senderId;
            
            ChatMessageResponse response = new ChatMessageResponse(
                -1L,
                roomId, // roomId를 직접 사용 (hex string 형식)
                senderId,
                username + "님이 채팅방에 입장했습니다.",
                OffsetDateTime.now(ZoneOffset.UTC),
                "SYSTEM"
            );
            
            messagingTemplate.convertAndSend("/topic/room." + roomId, response);
        } catch (Exception e) {
            ChatMessageResponse errorResponse = new ChatMessageResponse(
                -1L,
                "-1",
                -1L,
                "사용자 입장 처리 실패",
                OffsetDateTime.now(ZoneOffset.UTC),
                "ERROR"
            );
            
            messagingTemplate.convertAndSend("/topic/room." + roomId, errorResponse);
        }
    }

    /**
     * 사용자가 채팅방에서 나갔을 때
     */
    @MessageMapping("/chat.leaveUser/{roomId}")
    public void leaveUser(@DestinationVariable String roomId,
                         @Payload ChatMessageCreateRequest message) {
        try {
            Long senderId = message.senderId() != null ? message.senderId() : 1L;
            String username = message.content() != null ? message.content() : "사용자#" + senderId;
            
            ChatMessageResponse response = new ChatMessageResponse(
                -1L,
                roomId, // roomId를 직접 사용 (hex string 형식)
                senderId,
                username + "님이 채팅방에서 나갔습니다.",
                OffsetDateTime.now(ZoneOffset.UTC),
                "SYSTEM"
            );
            
            messagingTemplate.convertAndSend("/topic/room." + roomId, response);
        } catch (Exception e) {
            ChatMessageResponse errorResponse = new ChatMessageResponse(
                -1L,
                "-1",
                -1L,
                "사용자 퇴장 처리 실패",
                OffsetDateTime.now(ZoneOffset.UTC),
                "ERROR"
            );
            
            messagingTemplate.convertAndSend("/topic/room." + roomId, errorResponse);
        }
    }

    /**
     * 실시간 타이핑 상태 전송
     */
    @MessageMapping("/chat.typing/{roomId}")
    public void sendTypingStatus(@DestinationVariable String roomId,
                               @Payload ChatMessageCreateRequest message) {
        try {
            Long senderId = message.senderId() != null ? message.senderId() : 1L;
            String username = message.content() != null ? message.content() : "사용자#" + senderId;
            
            // 타이핑 상태를 해당 방의 다른 사용자들에게 전송
            String finalUsername = username;
            messagingTemplate.convertAndSend("/topic/room." + roomId + ".typing", 
                new Object() {
                    public final String type = "TYPING";
                    public final Long userId = senderId;
                    public final String username = finalUsername;
                    public final String message = finalUsername + "님이 타이핑중...";
                    public final String timestamp = Instant.now().toString();
                });
        } catch (Exception e) {
            // 타이핑 상태 에러는 무시
        }
    }

    /**
     * 개인 메시지 전송 (1:1 채팅)
     */
    @MessageMapping("/chat.private/{targetUserId}")
    public void sendPrivateMessage(@DestinationVariable String targetUserId,
                                 @Payload ChatMessageCreateRequest message) {
        try {
            Long senderId = message.senderId() != null ? message.senderId() : 1L;
            
            // 개인 채팅방 ID 생성 (작은 ID_큰 ID 형태)
            Long targetId = Long.parseLong(targetUserId);
            String privateRoomId = senderId < targetId ? 
                "private_" + senderId + "_" + targetId : 
                "private_" + targetId + "_" + senderId;
                
            // 메시지 저장 (privateRoomId를 직접 사용)
            String dbRoomId = privateRoomId;
            var savedMessage = chatMessageService.saveText(dbRoomId, senderId, message.content());
            
            // 수신자에게 메시지 전송
            ChatMessageResponse response = new ChatMessageResponse(
                savedMessage.id(),
                dbRoomId,
                savedMessage.senderId(),
                savedMessage.content(),
                OffsetDateTime.ofInstant(savedMessage.createdAt(), ZoneOffset.UTC),
                "PRIVATE"
            );
            
            messagingTemplate.convertAndSendToUser(
                targetUserId,
                "/queue/private",
                response
            );
            
            // 송신자에게도 확인 메시지 전송
            messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/queue/private",
                response
            );
            
        } catch (Exception e) {
            // 에러 메시지를 송신자에게 전송
            messagingTemplate.convertAndSendToUser(
                message.senderId().toString(),
                "/queue/errors",
                new Object() {
                    public final String error = "개인 메시지 전송 실패: " + e.getMessage();
                    public final String timestamp = Instant.now().toString();
                }
            );
        }
    }

}