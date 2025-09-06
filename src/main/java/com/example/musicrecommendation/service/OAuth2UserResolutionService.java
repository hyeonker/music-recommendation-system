package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.AuthProvider;
import com.example.musicrecommendation.domain.User;
import com.example.musicrecommendation.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuth2UserResolutionService {

    private final UserRepository userRepository;

    /**
     * OAuth2User로부터 데이터베이스의 User ID를 조회
     * 
     * @param oAuth2User OAuth2 인증 사용자 정보
     * @return 데이터베이스 사용자 ID (null if not found)
     */
    public Long resolveUserId(OAuth2User oAuth2User) {
        if (oAuth2User == null) {
            return null;
        }

        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        // Google 사용자 처리
        String googleSub = (String) attributes.get("sub");
        if (googleSub != null) {
            Optional<User> user = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, googleSub);
            if (user.isPresent()) {
                return user.get().getId();
            }
        }

        // Naver 사용자 처리
        Object responseObj = attributes.get("response");
        if (responseObj instanceof Map<?, ?> response) {
            String naverId = (String) response.get("id");
            if (naverId != null) {
                Optional<User> user = userRepository.findByProviderAndProviderId(AuthProvider.NAVER, naverId);
                if (user.isPresent()) {
                    return user.get().getId();
                }
            }
        }

        // Kakao 사용자 처리
        Object kakaoIdObj = attributes.get("id");
        if (kakaoIdObj != null) {
            String kakaoId = String.valueOf(kakaoIdObj);
            Optional<User> user = userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, kakaoId);
            if (user.isPresent()) {
                return user.get().getId();
            }
        }

        // 모든 provider 조회 실패 시 null 반환
        return null;
    }

    /**
     * OAuth2User가 어떤 provider인지 확인하고 providerId 추출
     * 디버깅용 메서드
     */
    public String getProviderInfo(OAuth2User oAuth2User) {
        if (oAuth2User == null) {
            return "NULL_USER";
        }

        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        if (attributes.containsKey("sub")) {
            return "GOOGLE:" + attributes.get("sub");
        }
        
        if (attributes.containsKey("response")) {
            Object response = attributes.get("response");
            if (response instanceof Map<?, ?> map) {
                return "NAVER:" + map.get("id");
            }
        }
        
        if (attributes.containsKey("id")) {
            return "KAKAO:" + attributes.get("id");
        }
        
        return "UNKNOWN:" + attributes.keySet();
    }
}