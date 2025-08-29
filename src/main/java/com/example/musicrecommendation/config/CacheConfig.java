package com.example.musicrecommendation.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cm = new CaffeineCacheManager(
                "artistSearch",
                "trackSearch",       // 트랙 검색 캐시
                "reviews",           // 리뷰 캐시
                "user-badges",       // 사용자 배지 캐시
                "music-items",       // 음악 아이템 캐시
                "review-stats",      // 리뷰 통계 캐시
                "user-names"         // 사용자 이름 캐시
        );
        cm.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(5000)                         // 최대 5000 키로 증가
                        .expireAfterWrite(Duration.ofMinutes(15))  // 15분 TTL
                        .recordStats()                             // 캐시 통계 기록
        );
        return cm;
    }
}
