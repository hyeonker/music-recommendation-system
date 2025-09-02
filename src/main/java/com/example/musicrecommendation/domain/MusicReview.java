package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 음악 리뷰
 */
@Entity
@Table(name = "music_reviews")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(of = "id")
public class MusicReview {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "music_item_id", nullable = false)
    private MusicItem musicItem;
    
    @Column(name = "rating", nullable = false)
    @Builder.Default
    private Integer rating = 5;
    
    @Column(name = "review_text", columnDefinition = "text")
    private String reviewText;
    
    @Column(name = "tags", columnDefinition = "text[]")
    private List<String> tags;
    
    @Column(name = "is_spoiler")
    @Builder.Default
    private Boolean isSpoiler = false;
    
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = true;
    
    @Column(name = "helpful_count")
    @Builder.Default
    private Integer helpfulCount = 0;
    
    @Column(name = "report_count")
    @Builder.Default
    private Integer reportCount = 0;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        updatedAt = OffsetDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        
        // 평점 유효성 검사
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1~5 사이의 값이어야 합니다.");
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
    }
    
    // 도움이 되는 리뷰인지 확인
    public boolean isHelpful() {
        return helpfulCount > 0;
    }
    
    // 신고가 많은 리뷰인지 확인
    public boolean isReported() {
        return reportCount > 3; // 3회 이상 신고 시 숨김 처리
    }
    
    // 짧은 리뷰인지 확인
    public boolean isShortReview() {
        return reviewText == null || reviewText.length() < 50;
    }
    
    // 평점별 이모지 반환
    public String getRatingEmoji() {
        return switch (rating) {
            case 1 -> "😞";
            case 2 -> "😕";
            case 3 -> "😐";
            case 4 -> "😊";
            case 5 -> "😍";
            default -> "⭐";
        };
    }
}