package com.example.musicrecommendation.web;

import com.example.musicrecommendation.domain.AuthProvider;
import com.example.musicrecommendation.domain.User;
import com.example.musicrecommendation.domain.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 현재 로그인 사용자 조회
     * - Google: email 이 거의 항상 존재
     * - Kakao/Naver: email이 없을 수 있어 provider+providerId로 조회
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        Map<String, Object> attrs = principal.getAttributes();
        // provider 추론: registrationId는 여기서 바로 못 가져오니, 속성 형태로 식별
        AuthProvider provider = resolveProvider(attrs);

        String email = null;
        String providerId = null;
        String name = null;
        String picture = null;

        switch (provider) {
            case GOOGLE -> {
                providerId = (String) attrs.get("sub");
                email = (String) attrs.get("email");
                name = (String) attrs.get("name");
                picture = (String) attrs.get("picture");
            }
            case KAKAO -> {
                providerId = String.valueOf(attrs.get("id"));
                Map<String, Object> account = safeMap(attrs.get("kakao_account"));
                Map<String, Object> profile = account != null ? safeMap(account.get("profile")) : null;
                email = account != null ? (String) account.get("email") : null; // 비즈앱 전환 전엔 null 가능
                name = profile != null ? (String) profile.getOrDefault("nickname", "Kakao User") : "Kakao User";
                picture = profile != null ? (String) profile.get("profile_image_url") : null;
            }
            case NAVER -> {
                Map<String, Object> resp = safeMap(attrs.get("response"));
                if (resp != null) {
                    providerId = (String) resp.get("id");
                    email = (String) resp.get("email");
                    name = (String) resp.getOrDefault("name", "Naver User");
                    picture = (String) resp.get("profile_image");
                }
            }
        }

        Optional<User> userOpt;
        if (providerId != null) {
            userOpt = userRepository.findByProviderAndProviderId(provider, providerId);
            if (userOpt.isEmpty() && email != null) {
                userOpt = userRepository.findByEmail(email);
            }
        } else {
            userOpt = (email != null) ? userRepository.findByEmail(email) : Optional.empty();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("authenticated", true);
        data.put("provider", provider.name());
        data.put("principal", attrs);
        data.put("user", userOpt.orElse(null));

        return ResponseEntity.ok(data);
    }

    /**
     * 세션 로그아웃 (프론트에서 호출)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // --- helpers ---

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object o) {
        if (o instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return null;
    }

    private AuthProvider resolveProvider(Map<String, Object> attrs) {
        // 구글은 "sub" 속성이 특징적
        if (attrs.containsKey("sub")) return AuthProvider.GOOGLE;
        // 카카오는 "id" 루트 + "kakao_account" 존재
        if (attrs.containsKey("id") && attrs.containsKey("kakao_account")) return AuthProvider.KAKAO;
        // 네이버는 "response" 내부에 id/email
        if (attrs.containsKey("response")) return AuthProvider.NAVER;
        // 기본값(안 나오도록 방어)
        return AuthProvider.GOOGLE;
    }
}
