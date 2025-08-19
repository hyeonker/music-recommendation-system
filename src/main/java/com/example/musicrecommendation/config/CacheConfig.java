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
        CaffeineCacheManager cm = new CaffeineCacheManager("artistSearch");
        cm.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(1000)                         // 최대 1000 키
                        .expireAfterWrite(Duration.ofMinutes(10))  // 10분 TTL
        );
        return cm;
    }
}
