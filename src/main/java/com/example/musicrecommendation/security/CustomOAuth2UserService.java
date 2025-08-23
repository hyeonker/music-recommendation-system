package com.example.musicrecommendation.security;

import com.example.musicrecommendation.domain.AuthProvider;
import com.example.musicrecommendation.domain.User;
import com.example.musicrecommendation.domain.UserRepository;
import com.example.musicrecommendation.service.UserPreferenceService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserPreferenceService userPreferenceService;

    public CustomOAuth2UserService(UserRepository userRepository,
                                   UserPreferenceService userPreferenceService) {
        this.userRepository = userRepository;
        this.userPreferenceService = userPreferenceService;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest req) {
        OAuth2User oAuth2User = super.loadUser(req);
        String registrationId = req.getClientRegistration().getRegistrationId(); // google/kakao/naver
        Map<String, Object> attrs = oAuth2User.getAttributes();

        AuthProvider provider = mapProvider(registrationId);

        String providerId = null;
        String email = null;
        String name = null;
        String picture = null;

        // --- Provider별 표준화 ---
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
                Map<String, Object> profile = safeMap(account != null ? account.get("profile") : null);
                email = account != null ? (String) account.get("email") : null; // 비즈앱 아니면 보통 null
                name = profile != null ? (String) profile.getOrDefault("nickname", "Kakao User") : "Kakao User";
                picture = profile != null ? (String) profile.get("profile_image_url") : null;
            }
            case NAVER -> {
                Map<String, Object> resp = safeMap(attrs.get("response"));
                providerId = resp != null ? (String) resp.get("id") : null;
                email = resp != null ? (String) resp.get("email") : null;
                name = resp != null ? (String) resp.getOrDefault("name", "Naver User") : "Naver User";
                picture = resp != null ? (String) resp.get("profile_image") : null;
            }
        }

        // 이메일이 없으면 NOT NULL 제약을 통과시키기 위한 대체 이메일 생성
        String effectiveEmail = (email != null && !email.isBlank())
                ? email
                : (provider.name().toLowerCase() + "_" + providerId + "@no-email.local");

        // --- 기존 사용자 조회 (우선순위: provider+providerId → email) ---
        Optional<User> existing = (providerId != null)
                ? userRepository.findByProviderAndProviderId(provider, providerId)
                : userRepository.findByEmail(effectiveEmail);

        User user;
        if (existing.isPresent()) {
            user = existing.get();
            // 재로그인 시 표시 정보 갱신
            user.updateProfile(
                    (name != null && !name.isBlank()) ? name : user.getName(),
                    (picture != null && !picture.isBlank()) ? picture : user.getProfileImageUrl()
            );
        } else {
            // 신규 사용자 저장 (대체 이메일 사용)
            String finalName = (name != null && !name.isBlank()) ? name : "User";
            user = userRepository.save(new User(
                    effectiveEmail,
                    finalName,
                    picture,
                    provider,
                    providerId
            ));
            // 최초 로그인 시 기본 취향 프로필 생성
            userPreferenceService.createOrUpdateUserProfile(user.getId());
        }

        // Security의 Principal은 기본 구현 유지
        return oAuth2User;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object obj) {
        if (obj instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return null;
    }

    private AuthProvider mapProvider(String id) {
        return switch (id.toLowerCase()) {
            case "google" -> AuthProvider.GOOGLE;
            case "kakao"  -> AuthProvider.KAKAO;
            case "naver"  -> AuthProvider.NAVER;
            default -> throw new IllegalArgumentException("Unknown provider: " + id);
        };
    }
}
