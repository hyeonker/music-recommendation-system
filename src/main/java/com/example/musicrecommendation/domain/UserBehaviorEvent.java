package com.example.musicrecommendation.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 사용자 행동 추적 이벤트
 * - 실시간 사용자 행동 데이터 수집
 * - ML 모델 학습용 데이터 제공
 * - 개인화 추천 정확도 향상
 */
@Entity
@Table(name = "user_behavior_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserBehaviorEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private ItemType itemType;
    
    @Column(name = "item_id", nullable = false)
    private String itemId;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "item_metadata", columnDefinition = "jsonb")
    private JsonNode itemMetadata;
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type")
    private DeviceType deviceType;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    public enum EventType {
        RECOMMENDATION_CLICK,  // 추천 곡 클릭 (YouTube/Spotify 이동)
        LIKE,                 // 좋아요 버튼 클릭
        DISLIKE,             // 싫어요 버튼 클릭
        SEARCH,              // 검색어 입력
        PLAYLIST_ADD,        // 플레이리스트에 추가
        REVIEW_WRITE,        // 리뷰 작성
        PROFILE_UPDATE,      // 프로필 수정 (선호 장르/아티스트)
        SESSION_START,       // 세션 시작
        SESSION_END,         // 세션 종료
        REFRESH_RECOMMENDATIONS // 추천 새로고침 클릭
    }
    
    public enum ItemType {
        SONG,
        ALBUM,
        ARTIST,
        PLAYLIST,
        GENRE
    }
    
    public enum DeviceType {
        WEB,
        MOBILE,
        DESKTOP,
        TABLET
    }
    
    /**
     * 긍정적 행동 여부 판단 (현실적 데이터 기반)
     */
    public boolean isPositiveEvent() {
        return eventType == EventType.LIKE
                || eventType == EventType.PLAYLIST_ADD
                || eventType == EventType.RECOMMENDATION_CLICK
                || (eventType == EventType.REVIEW_WRITE && getReviewRatingFromMetadata() >= 4.0);
    }
    
    /**
     * 부정적 행동 여부 판단 (현실적 데이터 기반)
     */
    public boolean isNegativeEvent() {
        return eventType == EventType.DISLIKE
                || (eventType == EventType.REVIEW_WRITE && getReviewRatingFromMetadata() <= 2.0);
    }
    
    /**
     * 암시적 피드백 점수 계산 (현실적 데이터 기반, 0.0 ~ 1.0)
     */
    public double getImplicitFeedbackScore() {
        return switch (eventType) {
            case LIKE -> 1.0;
            case PLAYLIST_ADD -> 0.9;
            case REVIEW_WRITE -> {
                double rating = getReviewRatingFromMetadata();
                if (rating > 0) yield rating / 5.0; // 5점 만점을 1.0 만점으로 변환
                else yield 0.7; // 기본 점수
            }
            case RECOMMENDATION_CLICK -> 0.7;  // 클릭은 관심 표현
            case SEARCH -> 0.5;  // 능동적 탐색
            case PROFILE_UPDATE -> 0.6;  // 적극적 개인화
            case REFRESH_RECOMMENDATIONS -> 0.4;  // 더 많은 옵션 원함
            case SESSION_START -> 0.3;
            case SESSION_END -> 0.3;
            case DISLIKE -> 0.0;
        };
    }
    
    /**
     * 메타데이터에서 리뷰 평점 추출
     */
    private double getReviewRatingFromMetadata() {
        if (itemMetadata != null && itemMetadata.has("rating")) {
            return itemMetadata.get("rating").asDouble();
        }
        return 0.0;
    }
}