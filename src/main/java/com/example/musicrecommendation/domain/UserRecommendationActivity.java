package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자 추천 활동 추적 엔티티
 * - 일일 새로고침 횟수 관리
 * - 서버 재시작에도 지속적인 데이터 보장
 * - 운영 모니터링 및 분석 지원
 */
@Entity
@Table(
    name = "user_recommendation_activity",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "activity_date"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserRecommendationActivity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;
    
    @Column(name = "refresh_count", nullable = false)
    @Builder.Default
    private Integer refreshCount = 0;
    
    @Column(name = "last_refresh_at")
    private LocalDateTime lastRefreshAt;
    
    @Column(name = "hourly_count", nullable = false)
    @Builder.Default
    private Integer hourlyCount = 0;
    
    @Column(name = "last_hourly_reset")
    private LocalDateTime lastHourlyReset;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 새로고침 횟수 증가 (일일 및 시간당)
     */
    public void incrementRefreshCount() {
        this.refreshCount++;
        this.lastRefreshAt = LocalDateTime.now();
        
        // 시간당 카운트 관리
        LocalDateTime now = LocalDateTime.now();
        if (this.lastHourlyReset == null || now.isAfter(this.lastHourlyReset.plusHours(1))) {
            // 시간당 카운트 리셋
            this.hourlyCount = 1;
            this.lastHourlyReset = now;
        } else {
            // 같은 시간대 내에서 카운트 증가
            this.hourlyCount++;
        }
    }
    
    /**
     * 일일 제한 초과 여부 확인
     */
    public boolean isLimitExceeded(int dailyLimit) {
        return this.refreshCount >= dailyLimit;
    }
    
    /**
     * 남은 새로고침 횟수 계산
     */
    public int getRemainingRefreshCount(int dailyLimit) {
        return Math.max(0, dailyLimit - this.refreshCount);
    }
    
    /**
     * 시간당 제한 초과 여부 확인
     */
    public boolean isHourlyLimitExceeded(int hourlyLimit) {
        // 시간 경과 확인
        LocalDateTime now = LocalDateTime.now();
        if (this.lastHourlyReset == null || now.isAfter(this.lastHourlyReset.plusHours(1))) {
            return false; // 새로운 시간대이므로 제한 없음
        }
        return this.hourlyCount >= hourlyLimit;
    }
    
    /**
     * 남은 시간당 새로고침 횟수 계산
     */
    public int getRemainingHourlyRefreshCount(int hourlyLimit) {
        LocalDateTime now = LocalDateTime.now();
        if (this.lastHourlyReset == null || now.isAfter(this.lastHourlyReset.plusHours(1))) {
            return hourlyLimit; // 새로운 시간대이므로 전체 허용
        }
        return Math.max(0, hourlyLimit - this.hourlyCount);
    }
    
    /**
     * 현재 시간당 사용 횟수 조회
     */
    public int getCurrentHourlyCount() {
        LocalDateTime now = LocalDateTime.now();
        if (this.lastHourlyReset == null || now.isAfter(this.lastHourlyReset.plusHours(1))) {
            return 0; // 새로운 시간대이므로 0
        }
        return this.hourlyCount;
    }
}