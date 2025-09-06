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
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 새로고침 횟수 증가
     */
    public void incrementRefreshCount() {
        this.refreshCount++;
        this.lastRefreshAt = LocalDateTime.now();
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
}