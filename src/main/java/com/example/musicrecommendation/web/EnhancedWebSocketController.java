package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.ChatRoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class EnhancedWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(EnhancedWebSocketController.class);

    private final ChatRoomService chatRoomService;

    public EnhancedWebSocketController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
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

    private Long extractUserIdFromPrincipal(Principal principal) {
        if (principal != null && principal.getName() != null) {
            try {
                return Long.parseLong(principal.getName());
            } catch (NumberFormatException ignored) { }
        }
        return 1L; // 데모 기본값
    }
}
