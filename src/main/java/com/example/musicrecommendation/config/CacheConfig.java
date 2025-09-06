package com.example.musicrecommendation.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 개선된 캐시 설정 - 캐시별 차별화된 TTL 및 크기 제한
 */
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final RecommendationProperties recommendationProperties;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cm = new CaffeineCacheManager();
        
        // 추천 캐시 (사용자별 개인화 추천)
        cm.registerCustomCache("musicRecommendations", 
                Caffeine.newBuilder()
                        .maximumSize(2000)
                        .expireAfterWrite(Duration.ofMinutes(recommendationProperties.getCacheTtlMinutes()))
                        .recordStats()
                        .build());
        
        // 새로고침 제한 상태 캐시 (짧은 TTL)
        cm.registerCustomCache("userRefreshStatus",
                Caffeine.newBuilder()
                        .maximumSize(10000)
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .recordStats()
                        .build());
        
        // Spotify API 응답 캐시 (긴 TTL)
        cm.registerCustomCache("spotifyApiCache",
                Caffeine.newBuilder()
                        .maximumSize(5000)
                        .expireAfterWrite(Duration.ofHours(1))
                        .recordStats()
                        .build());
        
        // 기존 캐시들 (기본 설정)
        String[] existingCaches = {
                "artistSearch", "trackSearch", "artistTrackSearch", 
                "reviews", "user-badges", "music-items", "review-stats", "user-names"
        };
        
        for (String cacheName : existingCaches) {
            cm.registerCustomCache(cacheName,
                    Caffeine.newBuilder()
                            .maximumSize(3000)
                            .expireAfterWrite(Duration.ofMinutes(15))
                            .recordStats()
                            .build());
        }
        
        return cm;
    }
}
