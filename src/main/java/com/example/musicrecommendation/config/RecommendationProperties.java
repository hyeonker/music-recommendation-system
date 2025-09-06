package com.example.musicrecommendation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 추천 시스템 설정 프로퍼티
 * - 모든 하드코딩된 값들을 외부 설정으로 관리
 * - 운영 환경에서 유연한 설정 변경 가능
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.recommendation")
public class RecommendationProperties {
    
    /**
     * 일일 새로고침 제한 횟수
     */
    private int dailyRefreshLimit = 10;
    
    /**
     * 한 번에 반환할 최대 추천 수
     */
    private int maxRecommendationsPerRequest = 8;
    
    /**
     * 캐시 TTL (분 단위)
     */
    private int cacheTtlMinutes = 60;
    
    /**
     * 폴백 추천 활성화 여부
     */
    private boolean fallbackEnabled = true;
    
    /**
     * 다양성 보장 설정
     */
    private Diversity diversity = new Diversity();
    
    /**
     * 추천 알고리즘 가중치 설정
     */
    private Weights weights = new Weights();
    
    @Data
    public static class Diversity {
        /**
         * 동일 아티스트 최대 곡 수
         */
        private int maxSameArtist = 2;
        
        /**
         * 동일 장르 최대 곡 수
         */
        private int maxSameGenre = 3;
    }
    
    @Data
    public static class Weights {
        /**
         * 컨텐츠 기반 필터링 가중치
         */
        private double contentBased = 0.4;
        
        /**
         * 협업 필터링 가중치
         */
        private double collaborative = 0.3;
        
        /**
         * 트렌딩 기반 가중치
         */
        private double trending = 0.3;
    }
}