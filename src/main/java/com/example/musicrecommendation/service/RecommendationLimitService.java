package com.example.musicrecommendation.service;

import com.example.musicrecommendation.config.RecommendationProperties;
import com.example.musicrecommendation.domain.UserRecommendationActivity;
import com.example.musicrecommendation.repository.UserRecommendationActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 추천 시스템 제한 관리 서비스
 * - 데이터베이스 기반 일일 새로고침 제한
 * - 서버 재시작에도 지속적인 제한 관리
 * - 성능 최적화를 위한 캐싱 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationLimitService {
    
    private final UserRecommendationActivityRepository activityRepository;
    private final RecommendationProperties properties;
    
    /**
     * 새로고침 가능 여부 확인 및 카운트 증가
     * 
     * @param userId 사용자 ID
     * @return 제한 정보와 성공 여부
     */
    /**
     * 현재 새로고침 상태 조회 (카운트 증가하지 않음)
     */
    @Transactional
    public RefreshLimitResult checkCurrentRefreshStatus(Long userId) {
        try {
            LocalDate today = LocalDate.now();
            UserRecommendationActivity activity = getOrCreateTodayActivity(userId, today);
            
            int dailyLimit = properties.getDailyRefreshLimit();
            
            return RefreshLimitResult.builder()
                .success(true)
                .currentCount(activity.getRefreshCount())
                .maxCount(dailyLimit)
                .remainingCount(Math.max(0, dailyLimit - activity.getRefreshCount()))
                .resetDate(today.plusDays(1))
                .message("현재 새로고침 상태 조회")
                .build();
                
        } catch (Exception e) {
            log.error("새로고침 상태 조회 실패: {}", e.getMessage(), e);
            
            // 기본값 반환
            return RefreshLimitResult.builder()
                .success(true)
                .currentCount(0)
                .maxCount(properties.getDailyRefreshLimit())
                .remainingCount(properties.getDailyRefreshLimit())
                .resetDate(LocalDate.now().plusDays(1))
                .message("기본 상태")
                .build();
        }
    }
    
    /**
     * 제한 상태만 확인 (카운트 증가 없음)
     */
    @Transactional(readOnly = true)
    public RefreshLimitResult checkLimitOnly(Long userId) {
        try {
            LocalDate today = LocalDate.now();
            UserRecommendationActivity activity = getOrCreateTodayActivity(userId, today);
            
            // 제한 상태만 확인
            int dailyLimit = properties.getDailyRefreshLimit();
            int hourlyLimit = 3; // 시간당 3회 제한
            
            return RefreshLimitResult.builder()
                .success(true)
                .currentCount(activity.getRefreshCount())
                .remainingCount(activity.getRemainingRefreshCount(dailyLimit))
                .maxCount(dailyLimit)
                .hourlyUsed(activity.getCurrentHourlyCount())
                .hourlyRemaining(activity.getRemainingHourlyRefreshCount(hourlyLimit))
                .hourlyMax(hourlyLimit)
                .resetDate(today.plusDays(1))
                .message("제한 상태 조회")
                .build();
                
        } catch (Exception e) {
            log.error("제한 상태 확인 실패 (userId: {}): {}", userId, e.getMessage());
            
            // 에러 시 기본값 반환
            return RefreshLimitResult.builder()
                .success(true)
                .currentCount(0)
                .remainingCount(properties.getDailyRefreshLimit())
                .maxCount(properties.getDailyRefreshLimit())
                .hourlyUsed(0)
                .hourlyRemaining(3)
                .hourlyMax(3)
                .resetDate(LocalDate.now().plusDays(1))
                .message("제한 확인 실패 - 기본값")
                .build();
        }
    }

    @Transactional
    public RefreshLimitResult checkAndIncrementRefreshCount(Long userId) {
        try {
            LocalDate today = LocalDate.now();
            UserRecommendationActivity activity = getOrCreateTodayActivity(userId, today);
            
            // 일일 제한 확인
            int dailyLimit = properties.getDailyRefreshLimit();
            int hourlyLimit = 3; // 시간당 3회 제한
            
            if (activity.isLimitExceeded(dailyLimit)) {
                log.info("사용자 {}의 일일 새로고침 제한 초과 (현재: {}, 제한: {})", 
                    userId, activity.getRefreshCount(), dailyLimit);
                
                return RefreshLimitResult.builder()
                    .success(false)
                    .currentCount(activity.getRefreshCount())
                    .remainingCount(0)
                    .maxCount(dailyLimit)
                    .hourlyUsed(activity.getCurrentHourlyCount())
                    .hourlyRemaining(activity.getRemainingHourlyRefreshCount(hourlyLimit))
                    .hourlyMax(hourlyLimit)
                    .resetDate(today.plusDays(1))
                    .message("일일 새로고침 제한에 도달했습니다")
                    .build();
            }
            
            // 시간당 제한 확인
            if (activity.isHourlyLimitExceeded(hourlyLimit)) {
                log.info("사용자 {}의 시간당 새로고침 제한 초과 (현재: {}, 제한: {})", 
                    userId, activity.getCurrentHourlyCount(), hourlyLimit);
                
                return RefreshLimitResult.builder()
                    .success(false)
                    .currentCount(activity.getRefreshCount())
                    .remainingCount(activity.getRemainingRefreshCount(dailyLimit))
                    .maxCount(dailyLimit)
                    .hourlyUsed(activity.getCurrentHourlyCount())
                    .hourlyRemaining(0)
                    .hourlyMax(hourlyLimit)
                    .resetDate(today.plusDays(1))
                    .message("시간당 새로고침 제한에 도달했습니다. 1시간 후에 다시 시도해주세요.")
                    .build();
            }
            
            // 카운트 증가 (일일 및 시간당)
            activity.incrementRefreshCount();
            activityRepository.save(activity);
            
            // 캐시 무효화 (다음 조회 시 최신 데이터 반영)
            evictUserActivityCache(userId, today);
            
            log.debug("사용자 {} 새로고침 카운트 증가: 일일 {}/{}, 시간당 {}/{}", 
                userId, activity.getRefreshCount(), dailyLimit, 
                activity.getCurrentHourlyCount(), hourlyLimit);
            
            return RefreshLimitResult.builder()
                .success(true)
                .currentCount(activity.getRefreshCount())
                .remainingCount(activity.getRemainingRefreshCount(dailyLimit))
                .maxCount(dailyLimit)
                .hourlyUsed(activity.getCurrentHourlyCount())
                .hourlyRemaining(activity.getRemainingHourlyRefreshCount(hourlyLimit))
                .hourlyMax(hourlyLimit)
                .resetDate(today.plusDays(1))
                .message("새로고침 성공")
                .build();
                
        } catch (Exception e) {
            log.error("새로고침 제한 확인 실패 (userId: {}): {}", userId, e.getMessage());
            
            // 에러 시 기본적으로 허용 (가용성 우선)
            return RefreshLimitResult.builder()
                .success(true)
                .currentCount(0)
                .remainingCount(properties.getDailyRefreshLimit())
                .maxCount(properties.getDailyRefreshLimit())
                .hourlyUsed(0)
                .hourlyRemaining(3)
                .hourlyMax(3)
                .resetDate(LocalDate.now().plusDays(1))
                .message("제한 확인 실패 - 기본 허용")
                .build();
        }
    }
    
    /**
     * 사용자의 오늘 새로고침 현황 조회
     */
    @Cacheable(value = "userRefreshStatus", key = "#userId + '_' + #date")
    @Transactional(readOnly = true)
    public RefreshStatus getCurrentRefreshStatus(Long userId, LocalDate date) {
        UserRecommendationActivity activity = activityRepository
            .findByUserIdAndActivityDate(userId, date)
            .orElse(null);
            
        int dailyLimit = properties.getDailyRefreshLimit();
        int currentCount = activity != null ? activity.getRefreshCount() : 0;
        
        return RefreshStatus.builder()
            .userId(userId)
            .date(date)
            .currentCount(currentCount)
            .remainingCount(Math.max(0, dailyLimit - currentCount))
            .maxCount(dailyLimit)
            .lastRefreshAt(activity != null ? activity.getLastRefreshAt() : null)
            .isLimitExceeded(currentCount >= dailyLimit)
            .build();
    }
    
    /**
     * 관리자용: 사용자 제한 리셋
     */
    @Transactional
    @CacheEvict(value = "userRefreshStatus", key = "#userId + '_' + T(java.time.LocalDate).now()")
    public void resetUserDailyLimit(Long userId) {
        LocalDate today = LocalDate.now();
        activityRepository.findByUserIdAndActivityDate(userId, today)
            .ifPresent(activity -> {
                activity.setRefreshCount(0);
                activity.setLastRefreshAt(null);
                activityRepository.save(activity);
                log.info("관리자가 사용자 {}의 일일 제한을 리셋했습니다", userId);
            });
    }
    
    /**
     * 오늘의 활동 기록 조회 또는 생성
     */
    private UserRecommendationActivity getOrCreateTodayActivity(Long userId, LocalDate today) {
        return activityRepository.findByUserIdAndActivityDate(userId, today)
            .orElseGet(() -> {
                UserRecommendationActivity newActivity = UserRecommendationActivity.builder()
                    .userId(userId)
                    .activityDate(today)
                    .refreshCount(0)
                    .build();
                return activityRepository.save(newActivity);
            });
    }
    
    /**
     * 사용자 활동 캐시 무효화
     */
    @CacheEvict(value = "userRefreshStatus", key = "#userId + '_' + #date")
    private void evictUserActivityCache(Long userId, LocalDate date) {
        // 캐시 무효화 전용 메서드
    }
    
    /**
     * 새로고침 제한 확인 결과
     */
    public static class RefreshLimitResult {
        private final boolean success;
        private final int currentCount;
        private final int remainingCount;
        private final int maxCount;
        private final int hourlyUsed;
        private final int hourlyRemaining;
        private final int hourlyMax;
        private final LocalDate resetDate;
        private final String message;
        
        public RefreshLimitResult(boolean success, int currentCount, int remainingCount, 
                                int maxCount, int hourlyUsed, int hourlyRemaining, int hourlyMax,
                                LocalDate resetDate, String message) {
            this.success = success;
            this.currentCount = currentCount;
            this.remainingCount = remainingCount;
            this.maxCount = maxCount;
            this.hourlyUsed = hourlyUsed;
            this.hourlyRemaining = hourlyRemaining;
            this.hourlyMax = hourlyMax;
            this.resetDate = resetDate;
            this.message = message;
        }
        
        public static RefreshLimitResultBuilder builder() {
            return new RefreshLimitResultBuilder();
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public int getCurrentCount() { return currentCount; }
        public int getRemainingCount() { return remainingCount; }
        public int getMaxCount() { return maxCount; }
        public int getHourlyUsed() { return hourlyUsed; }
        public int getHourlyRemaining() { return hourlyRemaining; }
        public int getHourlyMax() { return hourlyMax; }
        public LocalDate getResetDate() { return resetDate; }
        public String getMessage() { return message; }
        
        public Map<String, Object> toMap() {
            return Map.of(
                "success", success,
                "currentCount", currentCount,
                "remainingCount", remainingCount,
                "maxCount", maxCount,
                "hourlyUsed", hourlyUsed,
                "hourlyRemaining", hourlyRemaining,
                "hourlyMax", hourlyMax,
                "resetDate", resetDate.toString(),
                "message", message
            );
        }
        
        public static class RefreshLimitResultBuilder {
            private boolean success;
            private int currentCount;
            private int remainingCount;
            private int maxCount;
            private int hourlyUsed;
            private int hourlyRemaining;
            private int hourlyMax;
            private LocalDate resetDate;
            private String message;
            
            public RefreshLimitResultBuilder success(boolean success) {
                this.success = success;
                return this;
            }
            
            public RefreshLimitResultBuilder currentCount(int currentCount) {
                this.currentCount = currentCount;
                return this;
            }
            
            public RefreshLimitResultBuilder remainingCount(int remainingCount) {
                this.remainingCount = remainingCount;
                return this;
            }
            
            public RefreshLimitResultBuilder maxCount(int maxCount) {
                this.maxCount = maxCount;
                return this;
            }
            
            public RefreshLimitResultBuilder hourlyUsed(int hourlyUsed) {
                this.hourlyUsed = hourlyUsed;
                return this;
            }
            
            public RefreshLimitResultBuilder hourlyRemaining(int hourlyRemaining) {
                this.hourlyRemaining = hourlyRemaining;
                return this;
            }
            
            public RefreshLimitResultBuilder hourlyMax(int hourlyMax) {
                this.hourlyMax = hourlyMax;
                return this;
            }
            
            public RefreshLimitResultBuilder resetDate(LocalDate resetDate) {
                this.resetDate = resetDate;
                return this;
            }
            
            public RefreshLimitResultBuilder message(String message) {
                this.message = message;
                return this;
            }
            
            public RefreshLimitResult build() {
                return new RefreshLimitResult(success, currentCount, remainingCount, 
                                           maxCount, hourlyUsed, hourlyRemaining, hourlyMax, 
                                           resetDate, message);
            }
        }
    }
    
    /**
     * 새로고침 현황 정보
     */
    public static class RefreshStatus {
        private final Long userId;
        private final LocalDate date;
        private final int currentCount;
        private final int remainingCount;
        private final int maxCount;
        private final LocalDateTime lastRefreshAt;
        private final boolean isLimitExceeded;
        
        public RefreshStatus(Long userId, LocalDate date, int currentCount, int remainingCount,
                           int maxCount, LocalDateTime lastRefreshAt, boolean isLimitExceeded) {
            this.userId = userId;
            this.date = date;
            this.currentCount = currentCount;
            this.remainingCount = remainingCount;
            this.maxCount = maxCount;
            this.lastRefreshAt = lastRefreshAt;
            this.isLimitExceeded = isLimitExceeded;
        }
        
        public static RefreshStatusBuilder builder() {
            return new RefreshStatusBuilder();
        }
        
        // Getters
        public Long getUserId() { return userId; }
        public LocalDate getDate() { return date; }
        public int getCurrentCount() { return currentCount; }
        public int getRemainingCount() { return remainingCount; }
        public int getMaxCount() { return maxCount; }
        public LocalDateTime getLastRefreshAt() { return lastRefreshAt; }
        public boolean isLimitExceeded() { return isLimitExceeded; }
        
        public static class RefreshStatusBuilder {
            private Long userId;
            private LocalDate date;
            private int currentCount;
            private int remainingCount;
            private int maxCount;
            private LocalDateTime lastRefreshAt;
            private boolean isLimitExceeded;
            
            public RefreshStatusBuilder userId(Long userId) {
                this.userId = userId;
                return this;
            }
            
            public RefreshStatusBuilder date(LocalDate date) {
                this.date = date;
                return this;
            }
            
            public RefreshStatusBuilder currentCount(int currentCount) {
                this.currentCount = currentCount;
                return this;
            }
            
            public RefreshStatusBuilder remainingCount(int remainingCount) {
                this.remainingCount = remainingCount;
                return this;
            }
            
            public RefreshStatusBuilder maxCount(int maxCount) {
                this.maxCount = maxCount;
                return this;
            }
            
            public RefreshStatusBuilder lastRefreshAt(LocalDateTime lastRefreshAt) {
                this.lastRefreshAt = lastRefreshAt;
                return this;
            }
            
            public RefreshStatusBuilder isLimitExceeded(boolean isLimitExceeded) {
                this.isLimitExceeded = isLimitExceeded;
                return this;
            }
            
            public RefreshStatus build() {
                return new RefreshStatus(userId, date, currentCount, remainingCount, 
                                       maxCount, lastRefreshAt, isLimitExceeded);
            }
        }
    }
}