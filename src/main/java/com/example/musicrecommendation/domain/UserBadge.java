package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 사용자 배지/업적 시스템
 */
@Entity
@Table(name = "user_badges")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(of = {"userId", "badgeType"})
public class UserBadge {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "badge_type", nullable = false, length = 50)
    private BadgeType badgeType;
    
    @Column(name = "badge_name", nullable = false, length = 100)
    private String badgeName;
    
    @Column(name = "description", columnDefinition = "text")
    private String description;
    
    @Column(name = "icon_url", length = 500)
    private String iconUrl;
    
    @Column(name = "earned_at", nullable = false)
    private OffsetDateTime earnedAt;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    @PrePersist
    protected void onCreate() {
        if (earnedAt == null) {
            earnedAt = OffsetDateTime.now();
        }
    }
    
    public enum BadgeType {
        // 리뷰 관련 배지
        FIRST_REVIEW("첫 리뷰어", "🎵"),
        REVIEW_MASTER("리뷰 마스터", "📝"),
        HELPFUL_REVIEWER("도움이 되는 리뷰어", "👍"),
        CRITIC("음악 평론가", "🎭"),
        
        // 음악 탐험 배지
        GENRE_EXPLORER("장르 탐험가", "🗺️"),
        EARLY_ADOPTER("얼리 어답터", "⚡"),
        MUSIC_DISCOVERER("음악 발굴자", "💎"),
        
        // 소셜 활동 배지
        SOCIAL_BUTTERFLY("소셜 나비", "🦋"),
        FRIEND_MAKER("친구 메이커", "🤝"),
        CHAT_MASTER("대화의 달인", "💬"),
        
        // 특별 배지
        BETA_TESTER("베타 테스터", "🧪"),
        ANNIVERSARY("기념일", "🎉"),
        SPECIAL_EVENT("특별 이벤트", "🌟");
        
        private final String defaultName;
        private final String defaultIcon;
        
        BadgeType(String defaultName, String defaultIcon) {
            this.defaultName = defaultName;
            this.defaultIcon = defaultIcon;
        }
        
        public String getDefaultName() { return defaultName; }
        public String getDefaultIcon() { return defaultIcon; }
    }
    
    // 배지 희귀도 계산
    public String getRarity() {
        return switch (badgeType) {
            case FIRST_REVIEW, SOCIAL_BUTTERFLY -> "COMMON";
            case REVIEW_MASTER, GENRE_EXPLORER, FRIEND_MAKER -> "UNCOMMON";
            case HELPFUL_REVIEWER, CRITIC, EARLY_ADOPTER -> "RARE";
            case MUSIC_DISCOVERER, CHAT_MASTER -> "EPIC";
            case BETA_TESTER, ANNIVERSARY, SPECIAL_EVENT -> "LEGENDARY";
        };
    }
    
    // 배지 색상
    public String getBadgeColor() {
        return switch (getRarity()) {
            case "COMMON" -> "#95A5A6";      // 회색
            case "UNCOMMON" -> "#2ECC71";    // 초록
            case "RARE" -> "#3498DB";        // 파랑
            case "EPIC" -> "#9B59B6";        // 보라
            case "LEGENDARY" -> "#F1C40F";   // 금색
            default -> "#BDC3C7";
        };
    }
}