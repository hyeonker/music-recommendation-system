package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.ChatRoomService;
import com.example.musicrecommendation.service.SecureChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final SecureChatRoomService secureChatRoomService;
    private final com.example.musicrecommendation.service.UserService userService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final com.example.musicrecommendation.domain.UserRepository userRepository;

    @GetMapping("/rooms")
    public ResponseEntity<?> getRooms(@AuthenticationPrincipal OAuth2User oAuth2User) {
        // 인증된 사용자의 채팅방 목록 조회
        Long userId = getCurrentUserId(oAuth2User);
        if (userId == null) {
            return ResponseEntity.status(401).body("인증이 필요합니다");
        }
        
        Object userChatRoom = chatRoomService.getUserChatRoom(userId);
        return ResponseEntity.ok(java.util.List.of(userChatRoom));
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable String roomId, @AuthenticationPrincipal OAuth2User oAuth2User) {
        // 사용자 인증 확인
        Long userId = getCurrentUserId(oAuth2User);
        if (userId == null) {
            return ResponseEntity.status(401).body("인증이 필요합니다");
        }
        
        // 채팅방 접근 권한 확인
        if (!secureChatRoomService.hasRoomAccess(roomId, userId)) {
            return ResponseEntity.status(403).body("채팅방 접근 권한이 없습니다");
        }
        
        Object chatHistory = chatRoomService.getChatHistory(roomId, userId);
        return ResponseEntity.ok(chatHistory);
    }

    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable String roomId, @AuthenticationPrincipal OAuth2User oAuth2User) {
        // 사용자 인증 확인
        Long userId;
        try {
            userId = getCurrentUserId(oAuth2User);
        } catch (Exception e) {
            System.err.println("사용자 인증 실패: " + e.getMessage());
            return ResponseEntity.status(401).body("인증이 필요합니다");
        }
        
        // 채팅방 접근 권한 확인 (나가기 전에 참여자인지 확인)
        if (!secureChatRoomService.hasRoomAccess(roomId, userId)) {
            return ResponseEntity.status(403).body("채팅방에 참여하고 있지 않습니다");
        }
        
        // 사용자 정보 조회 (나가기 메시지용)
        String userName = "사용자";
        try {
            var user = userService.findUserByEmail(oAuth2User.getAttribute("email"));
            if (user.isPresent()) {
                userName = user.get().getName() != null ? user.get().getName() : "사용자";
            }
        } catch (Exception e) {
            System.err.println("사용자 이름 조회 실패: " + e.getMessage());
        }
        
        // 채팅방 나가기 처리
        boolean success = secureChatRoomService.leaveRoom(roomId, userId);
        
        if (success) {
            // WebSocket을 통해 시스템 메시지로 나가기 알림 전송
            try {
                final String finalRoomId = roomId;
                final String finalUserName = userName;
                final Object leaveNotification = new Object() {
                    public final String id = String.valueOf(System.currentTimeMillis());
                    public final String roomId = finalRoomId;
                    public final int senderId = 0; // 시스템 메시지
                    public final String senderName = "시스템";
                    public final String content = finalUserName + "님이 채팅방을 나갔습니다.";
                    public final String timestamp = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString();
                    public final String createdAt = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString();
                };
                
                // WebSocket을 통해 브로드캐스트
                messagingTemplate.convertAndSend("/topic/room." + roomId, leaveNotification);
                System.out.println("✅ 나가기 알림 브로드캐스트 완료: " + roomId);
                
            } catch (Exception e) {
                System.err.println("나가기 알림 전송 실패: " + e.getMessage());
            }
            
            return ResponseEntity.ok("채팅방에서 나가기 완료");
        } else {
            return ResponseEntity.status(500).body("채팅방 나가기 실패");
        }
    }

    private Long getCurrentUserId(OAuth2User oauth2User) {
        if (oauth2User == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }
        
        System.out.println("=== OAuth2User 분석 시작 ===");
        System.out.println("OAuth2User attributes: " + oauth2User.getAttributes());
        System.out.println("OAuth2User name: " + oauth2User.getName());
        
        // OAuth2User에서 provider와 providerId를 추출하여 데이터베이스에서 사용자 조회
        java.util.Map<String, Object> attrs = oauth2User.getAttributes();
        
        // Google OAuth2의 경우
        String providerId = (String) attrs.get("sub");  // Google의 경우 "sub"이 providerId
        String email = (String) attrs.get("email");
        
        // Kakao OAuth2의 경우
        if (providerId == null && attrs.get("id") != null) {
            providerId = String.valueOf(attrs.get("id")); // Kakao의 경우 "id"가 providerId
        }
        
        // Naver OAuth2의 경우
        if (providerId == null) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> response = (java.util.Map<String, Object>) attrs.get("response");
            if (response != null) {
                providerId = (String) response.get("id"); // Naver의 경우 response.id가 providerId
                if (email == null) {
                    email = (String) response.get("email");
                }
            }
        }
        
        System.out.println("추출된 정보 - providerId: " + providerId + ", email: " + email);
        
        // 데이터베이스에서 사용자 찾기
        if (providerId != null) {
            // Google 사용자 먼저 시도
            var googleUser = userRepository.findByProviderAndProviderId(
                com.example.musicrecommendation.domain.AuthProvider.GOOGLE, providerId);
            if (googleUser.isPresent()) {
                System.out.println("✅ Google 사용자 발견 - ID: " + googleUser.get().getId());
                return googleUser.get().getId();
            }
            
            // Kakao 사용자 시도
            var kakaoUser = userRepository.findByProviderAndProviderId(
                com.example.musicrecommendation.domain.AuthProvider.KAKAO, providerId);
            if (kakaoUser.isPresent()) {
                System.out.println("✅ Kakao 사용자 발견 - ID: " + kakaoUser.get().getId());
                return kakaoUser.get().getId();
            }
            
            // Naver 사용자 시도
            System.out.println("🔍 Naver 사용자 검색 중 - providerId: " + providerId);
            var naverUser = userRepository.findByProviderAndProviderId(
                com.example.musicrecommendation.domain.AuthProvider.NAVER, providerId);
            if (naverUser.isPresent()) {
                System.out.println("✅ Naver 사용자 발견 - ID: " + naverUser.get().getId());
                return naverUser.get().getId();
            } else {
                System.out.println("❌ Naver 사용자 없음 - providerId: " + providerId);
            }
        }
        
        // 이메일로 사용자 찾기 (providerId가 없는 경우의 대체 방법)
        if (email != null) {
            System.out.println("🔍 이메일로 사용자 검색 중 - email: " + email);
            var userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isPresent()) {
                System.out.println("✅ 이메일로 사용자 발견 - ID: " + userByEmail.get().getId());
                return userByEmail.get().getId();
            } else {
                System.out.println("❌ 이메일로 사용자 없음 - email: " + email);
            }
        }
        
        System.err.println("데이터베이스에서 사용자를 찾을 수 없습니다. providerId: " + providerId + ", email: " + email + ", attributes: " + attrs);
        throw new IllegalStateException("데이터베이스에서 사용자를 찾을 수 없습니다. providerId: " + providerId + ", email: " + email);
    }
    
    // roomId 정규화 헬퍼 메소드 (ChatMessageController와 동일한 로직)
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