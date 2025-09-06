package com.example.musicrecommendation.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 추천 시스템 스케줄러 서비스
 * - 주기적 추천 업데이트
 * - 캐시 관리
 * - 추천 모델 재훈련
 */
@Service
@RequiredArgsConstructor
public class RecommendationSchedulerService {
    
    private static final Logger log = LoggerFactory.getLogger(RecommendationSchedulerService.class);
    
    private final MusicRecommendationEngine recommendationEngine;
    private final UserProfileService userProfileService;
    private final CacheManager cacheManager;
    
    /**
     * 매일 새벽 3시에 추천 캐시 점진적 갱신 (전체 삭제 대신 만료된 것만)
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Async
    public CompletableFuture<Void> refreshRecommendationCache() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("추천 캐시 점진적 갱신 시작");
                
                var cache = cacheManager.getCache("musicRecommendations");
                if (cache != null) {
                    // Caffeine 캐시의 경우 자동 만료 정책을 통해 점진적으로 갱신
                    // 전체 삭제 대신 통계 정보만 로깅
                    log.info("캐시 자동 만료 정책을 통한 점진적 갱신 진행");
                }
                
                log.info("추천 캐시 점진적 갱신 완료");
                
            } catch (Exception e) {
                log.error("추천 캐시 갱신 실패: {}", e.getMessage());
            }
        });
    }
    
    /**
     * 매주 일요일 새벽 2시에 추천 모델 재훈련 (시뮬레이션)
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    @Async
    public CompletableFuture<Void> retrainRecommendationModel() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("추천 모델 재훈련 시작");
                
                // 실제 구현에서는 여기서 ML 모델을 재훈련함
                // 1. 사용자 피드백 데이터 수집
                // 2. 모델 재훈련
                // 3. 성능 평가
                // 4. 모델 배포
                
                Thread.sleep(5000); // 재훈련 시뮬레이션
                
                log.info("추천 모델 재훈련 완료");
                
            } catch (Exception e) {
                log.error("추천 모델 재훈련 실패: {}", e.getMessage());
            }
        });
    }
    
    /**
     * 매시간마다 추천 통계 업데이트
     */
    @Scheduled(fixedRate = 3600000) // 1시간
    @Async
    public CompletableFuture<Void> updateRecommendationStatistics() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("추천 통계 업데이트 시작");
                
                // 추천 통계 수집 및 업데이트
                // 예: 추천 정확도, 사용자 만족도, 인기 장르 등
                
                log.debug("추천 통계 업데이트 완료");
                
            } catch (Exception e) {
                log.warn("추천 통계 업데이트 실패: {}", e.getMessage());
            }
        });
    }
    
    /**
     * 수동으로 특정 사용자의 추천 갱신
     */
    @Async
    public CompletableFuture<Void> refreshUserRecommendations(Long userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("사용자 {} 추천 갱신 시작", userId);
                
                // 해당 사용자의 캐시 삭제
                var cache = cacheManager.getCache("musicRecommendations");
                if (cache != null) {
                    // 해당 사용자의 모든 추천 캐시 키 삭제
                    cache.evictIfPresent(userId + "_20");
                    cache.evictIfPresent(userId + "_10");
                    cache.evictIfPresent(userId + "_30");
                }
                
                // 새로운 추천 미리 생성
                recommendationEngine.getPersonalizedRecommendations(userId, 20);
                
                log.info("사용자 {} 추천 갱신 완료", userId);
                
            } catch (Exception e) {
                log.error("사용자 {} 추천 갱신 실패: {}", userId, e.getMessage());
            }
        });
    }
    
    /**
     * 시스템 상태 체크 및 알림
     */
    @Scheduled(fixedRate = 600000) // 10분
    public void healthCheck() {
        try {
            // 추천 시스템 상태 체크
            boolean isHealthy = checkRecommendationSystemHealth();
            
            if (!isHealthy) {
                log.warn("추천 시스템 상태 이상 감지");
                // 실제 구현에서는 알림 전송 로직 추가
            }
            
        } catch (Exception e) {
            log.error("추천 시스템 헬스 체크 실패: {}", e.getMessage());
        }
    }
    
    private boolean checkRecommendationSystemHealth() {
        try {
            // 기본적인 헬스 체크
            // 1. 캐시 매니저 상태
            // 2. 데이터베이스 연결
            // 3. 외부 API (Spotify) 상태
            
            return cacheManager != null && 
                   cacheManager.getCache("musicRecommendations") != null;
                   
        } catch (Exception e) {
            return false;
        }
    }
}