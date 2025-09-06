package com.example.musicrecommendation.config;

import com.example.musicrecommendation.security.CustomOAuth2UserService;
import com.example.musicrecommendation.security.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler successHandler;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
                          OAuth2LoginSuccessHandler successHandler) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.successHandler = successHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 🔐 엔드포인트 접근 제어
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/error",
                                "/favicon.ico",               // 파비콘 404 소음 제거
                                "/ws/**", "/ws-native/**",    // SockJS/WS 핸드셰이크 & 폴링
                                "/oauth2/**", "/login/**",
                                "/api/auth/**", "/api/auth/local/**",
                                "/api/public/**",
                                "/api/realtime-matching/**",  // 매칭 API 허용
                                "/api/chat/**",               // 채팅 API 허용
                                "/api/user-profile/**",       // 프로필 API 허용
                                "/api/users/**",              // 사용자 API 허용
                                "/api/spotify/**",            // Spotify API 허용
                                "/api/recommendations/**",    // 추천 API 허용
                                "/api/reviews/recent",        // 공개 리뷰 목록 조회 허용
                                "/api/reviews/helpful",       // 도움이 되는 리뷰 목록 허용
                                "/api/reviews/high-rated",    // 높은 평점 리뷰 목록 허용
                                "/api/reviews/music-item/*/rating", // 평점 평균 조회 허용
                                "/api/reviews/music-item/*/count",   // 리뷰 개수 조회 허용
                                "/api/reviews/tags",          // 태그 목록 조회 허용
                                "/api/badges/**",             // 배지 API 허용 (디버그용)
                                "/api/likes/**",              // 좋아요 API 허용
                                "/api/admin/**",              // 관리자 API 허용 (컨트롤러에서 권한 체크)
                                "/actuator/**",
                                "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // 🛡️ CSRF는 켜두되 SockJS 경로만 예외 처리 (xhr_send 등 POST 보호 회피)
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        new AntPathRequestMatcher("/ws/**"),
                        new AntPathRequestMatcher("/ws-native/**"),
                        new AntPathRequestMatcher("/api/realtime-matching/**"),
                        new AntPathRequestMatcher("/api/chat/**"),
                        new AntPathRequestMatcher("/api/user-profile/**"),
                        new AntPathRequestMatcher("/api/users/**"),
                        new AntPathRequestMatcher("/api/spotify/**"),
                        new AntPathRequestMatcher("/api/reviews/**"),
                        new AntPathRequestMatcher("/api/badges/**"),
                        new AntPathRequestMatcher("/api/recommendations/**"),
                        new AntPathRequestMatcher("/api/likes/**"),
                        new AntPathRequestMatcher("/api/admin/**"),
                        new AntPathRequestMatcher("/api/notifications/**"),
                        new AntPathRequestMatcher("/api/auth/local/**")
                ))

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ✅ OAuth2 로그인
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                        .successHandler(successHandler)
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        // 예: http://localhost:3000
        cors.setAllowedOrigins(List.of(allowedOrigin));
        cors.setAllowedMethods(Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);
        
        // 🔧 커스텀 헤더 노출 설정 - 새로고침 제한 정보용
        cors.setExposedHeaders(Arrays.asList(
            "X-Refresh-Used", 
            "X-Refresh-Remaining", 
            "X-Refresh-Max", 
            "X-Refresh-Reset-Date",
            "X-Hourly-Used",
            "X-Hourly-Remaining", 
            "X-Hourly-Max"
        ));

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cors);
        return src;
    }
}
