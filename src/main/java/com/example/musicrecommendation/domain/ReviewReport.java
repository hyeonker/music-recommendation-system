package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * 리뷰 신고
 */
@Entity
@Table(name = "review_reports")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(of = "id")
public class ReviewReport {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "review_id", nullable = false)
    private Long reviewId;
    
    @Column(name = "reporter_user_id", nullable = false)
    private Long reporterUserId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 50)
    private ReportReason reason;
    
    @Column(name = "description", columnDefinition = "text")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;
    
    @Column(name = "resolved_by_user_id")
    private Long resolvedByUserId;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
    
    public enum ReportReason {
        SPAM("스팸"),
        INAPPROPRIATE("부적절한 내용"),
        HATE_SPEECH("혐오 발언"),
        FAKE_REVIEW("가짜 리뷰"),
        COPYRIGHT("저작권 침해"),
        OTHER("기타");
        
        private final String displayName;
        
        ReportReason(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum ReportStatus {
        PENDING("처리 대기"),
        REVIEWING("검토 중"),
        RESOLVED("해결됨"),
        DISMISSED("기각됨");
        
        private final String displayName;
        
        ReportStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}