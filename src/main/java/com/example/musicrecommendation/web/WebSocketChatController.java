package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.ChatMessageService;
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

    /**
     * 특정 채팅방에 메시지 전송
     * 클라이언트에서 /app/chat.sendMessage/{roomId} 로 메시지 전송
     */
    @MessageMapping("/chat.sendMessage/{roomId}")
    @SendTo("/topic/room.{roomId}")
    public Object sendMessage(@DestinationVariable String roomId,
                            @Payload ChatMessageCreateRequest message) {
        try {
            System.out.println("🔥 WebSocketChatController: 채팅 메시지 수신 - roomId: " + roomId + ", 내용: " + message.content());
            
            // roomId를 정규화 (room_2_2 -> 22)
            Long normalizedRoomId = normalizeRoomId(roomId);
            Long senderId = message.senderId() != null ? message.senderId() : 1L;
            
            System.out.println("🔥 WebSocketChatController: 정규화된 roomId: " + normalizedRoomId + ", senderId: " + senderId);
            
            // 메시지 저장
            var savedMessage = chatMessageService.saveText(normalizedRoomId, senderId, message.content());
            
            // 사용자 이름 조회
            String userName = userService.findUserById(senderId)
                .map(user -> user.getName())
                .orElse("사용자#" + senderId);
            
            System.out.println("🔥 WebSocketChatController: 사용자 이름: " + userName);
            
            // 프론트엔드 ChatMessage 인터페이스와 일치하는 응답 생성
            final String finalRoomId = normalizedRoomId.toString();
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
            return response;
            
        } catch (Exception e) {
            // 에러 메시지 전송
            final String finalRoomId = roomId;
            return new Object() {
                public final String id = "-1";
                public final String roomId = finalRoomId;
                public final int senderId = -1;
                public final String senderName = "System";
                public final String content = "메시지 전송 실패: " + e.getMessage();
                public final String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
            };
        }
    }

    /**
     * 사용자가 채팅방에 입장했을 때
     */
    @MessageMapping("/chat.addUser/{roomId}")
    @SendTo("/topic/room.{roomId}")
    public ChatMessageResponse addUser(@DestinationVariable String roomId,
                                     @Payload ChatMessageCreateRequest message) {
        try {
            Long senderId = message.senderId() != null ? message.senderId() : 1L;
            String username = message.content() != null ? message.content() : "사용자#" + senderId;
            
            return new ChatMessageResponse(
                -1L,
                normalizeRoomId(roomId),
                senderId,
                username + "님이 채팅방에 입장했습니다.",
                OffsetDateTime.now(ZoneOffset.UTC),
                "SYSTEM"
            );
        } catch (Exception e) {
            return new ChatMessageResponse(
                -1L,
                -1L,
                -1L,
                "사용자 입장 처리 실패",
                OffsetDateTime.now(ZoneOffset.UTC),
                "ERROR"
            );
        }
    }

    /**
     * 사용자가 채팅방에서 나갔을 때
     */
    @MessageMapping("/chat.leaveUser/{roomId}")
    @SendTo("/topic/room.{roomId}")
    public ChatMessageResponse leaveUser(@DestinationVariable String roomId,
                                       @Payload ChatMessageCreateRequest message) {
        try {
            Long senderId = message.senderId() != null ? message.senderId() : 1L;
            String username = message.content() != null ? message.content() : "사용자#" + senderId;
            
            return new ChatMessageResponse(
                -1L,
                normalizeRoomId(roomId),
                senderId,
                username + "님이 채팅방에서 나갔습니다.",
                OffsetDateTime.now(ZoneOffset.UTC),
                "SYSTEM"
            );
        } catch (Exception e) {
            return new ChatMessageResponse(
                -1L,
                -1L,
                -1L,
                "사용자 퇴장 처리 실패",
                OffsetDateTime.now(ZoneOffset.UTC),
                "ERROR"
            );
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
                
            // 메시지 저장
            Long normalizedRoomId = normalizeRoomId(privateRoomId);
            var savedMessage = chatMessageService.saveText(normalizedRoomId, senderId, message.content());
            
            // 수신자에게 메시지 전송
            ChatMessageResponse response = new ChatMessageResponse(
                savedMessage.id(),
                normalizedRoomId,
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

    /**
     * roomId 정규화 헬퍼 메소드
     */
    private Long normalizeRoomId(String roomId) {
        if (roomId == null) return 1L;
        
        // "room_1_2" -> "12" 또는 해시값으로 변환
        String digits = roomId.replaceAll("\\D", "");
        if (!digits.isEmpty()) {
            try {
                return Long.parseLong(digits.length() > 10 ? digits.substring(0, 10) : digits);
            } catch (NumberFormatException e) {
                // 파싱 실패시 해시값 사용
                return (long) Math.abs(roomId.hashCode());
            }
        }
        
        // 숫자가 없으면 해시값 사용
        return (long) Math.abs(roomId.hashCode());
    }
}