package com.example.musicrecommendation.config;

import com.example.musicrecommendation.security.CustomOAuth2UserService;
import com.example.musicrecommendation.security.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
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
                        // ✅ 완전 공개
                        .requestMatchers(
                                "/", "/error",
                                "/ws/**", "/ws-native/**",          // WebSocket Handshake
                                "/oauth2/**", "/login/**",          // OAuth2 흐름
                                "/api/auth/**",                     // 로그인 상태 확인/로그아웃
                                "/api/public/**",                   // 공개 API가 있다면 여기로
                                "/actuator/**",
                                "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
                        ).permitAll()

                        // (필요 시) 공개 GET API 예시 — 주석 해제해서 사용
                        // .requestMatchers(HttpMethod.GET, "/api/spotify/**").permitAll()

                        // 그 외 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                // 개발 단계: CSRF 비활성(세션/OAuth 조합일 때 폼 POST 아니면 보통 이렇게 둠)
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())

                // ✅ OAuth2 로그인: 사용자 정보 로딩 + 성공 시 프론트로 리다이렉트
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                        .successHandler(successHandler)
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(allowedOrigin)); // http://localhost:3000
        cors.setAllowedMethods(Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cors);
        return src;
    }
}
