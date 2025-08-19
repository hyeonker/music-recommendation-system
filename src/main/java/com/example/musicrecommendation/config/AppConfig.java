package com.example.musicrecommendation.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(SpotifyProperties.class) // ← 여기서만 등록
public class AppConfig {

    // WebClient.Builder는 Spring Boot가 자동으로도 만들어주지만
    // 명시적으로 써도 무방합니다.
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    // ⚠️ 절대 이렇게 만들지 마세요:
    // @Bean
    // public SpotifyProperties spotifyProperties() { ... }  ← 이게 중복 원인
}
