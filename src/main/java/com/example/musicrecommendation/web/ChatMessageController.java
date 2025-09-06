package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.ChatMessageService;
import com.example.musicrecommendation.service.SecureChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.example.musicrecommendation.web.dto.ChatMessageCreateRequest;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final SecureChatRoomService secureChatRoomService;
    private final com.example.musicrecommendation.service.UserService userService;

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "asc") String order,
            @AuthenticationPrincipal OAuth2User oAuth2User
    ) {
        // 사용자 인증 확인
        Long userId = getCurrentUserId(oAuth2User);
        if (userId == null) {
            return ResponseEntity.status(401).body("인증이 필요합니다");
        }
        
        // 채팅방 접근 권한 확인
        if (!secureChatRoomService.hasRoomAccess(roomId, userId)) {
            return ResponseEntity.status(403).body("채팅방 접근 권한이 없습니다");
        }
        
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        boolean asc = !"desc".equalsIgnoreCase(order);
        return ResponseEntity.ok(chatMessageService.getMessages(roomId, safeLimit, asc));
    }

    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<?> postMessage(
            @PathVariable String roomId,
            @RequestParam(required = false, defaultValue = "") String content,
            @AuthenticationPrincipal OAuth2User oAuth2User
    ) {
        // 사용자 인증 확인
        Long userId = getCurrentUserId(oAuth2User);
        if (userId == null) {
            return ResponseEntity.status(401).body("인증이 필요합니다");
        }
        
        // 채팅방 접근 권한 확인
        if (!secureChatRoomService.hasRoomAccess(roomId, userId)) {
            return ResponseEntity.status(403).body("채팅방 접근 권한이 없습니다");
        }
        
        if (content.isEmpty()) {
            return ResponseEntity.badRequest().body("메시지 내용이 필요합니다");
        }
        
        // 채팅방 활동 업데이트
        secureChatRoomService.updateRoomActivity(roomId);
        
        chatMessageService.saveText(roomId, userId, content);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/rooms/{roomId}/messages", consumes = "application/json")
    public ResponseEntity<?> postMessageJson(
            @PathVariable String roomId,
            @RequestBody ChatMessageCreateRequest request,
            @AuthenticationPrincipal OAuth2User oAuth2User
    ) {
        // 사용자 인증 확인
        Long userId = getCurrentUserId(oAuth2User);
        if (userId == null) {
            return ResponseEntity.status(401).body("인증이 필요합니다");
        }
        
        // 채팅방 접근 권한 확인
        if (!secureChatRoomService.hasRoomAccess(roomId, userId)) {
            return ResponseEntity.status(403).body("채팅방 접근 권한이 없습니다");
        }
        
        if (request.content().isEmpty()) {
            return ResponseEntity.badRequest().body("메시지 내용이 필요합니다");
        }
        
        // 채팅방 활동 업데이트
        secureChatRoomService.updateRoomActivity(roomId);
        
        chatMessageService.saveText(roomId, userId, request.content());
        return ResponseEntity.ok().build();
    }
    
    // 현재 로그인된 사용자 ID 조회
    private Long getCurrentUserId(OAuth2User oAuth2User) {
        if (oAuth2User == null) return null;
        
        try {
            String email = oAuth2User.getAttribute("email");
            if (email != null) {
                return userService.findUserByEmail(email)
                    .map(user -> user.getId())
                    .orElse(null);
            }
        } catch (Exception e) {
            System.err.println("사용자 ID 조회 실패: " + e.getMessage());
        }
        return null;
    }
    
}
