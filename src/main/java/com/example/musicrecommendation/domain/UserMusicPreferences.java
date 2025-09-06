package com.example.musicrecommendation.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 사용자 음악 취향 프로필 (집계 데이터)
 * - 실시간 행동 데이터를 기반으로 계산된 선호도
 * - 빠른 추천 생성을 위한 사전 계산된 데이터
 */
@Entity
@Table(name = "user_music_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserMusicPreferences {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    /**
     * 장르 선호도 (JSON)
     * 예: {"rock": 0.8, "pop": 0.6, "jazz": 0.3}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "genre_preferences", columnDefinition = "jsonb")
    private JsonNode genrePreferences;
    
    /**
     * 아티스트 선호도 (JSON)
     * 예: {"artist_id_1": 0.9, "artist_id_2": 0.7}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "artist_preferences", columnDefinition = "jsonb")
    private JsonNode artistPreferences;
    
    /**
     * 음악적 특성 선호도 (JSON)
     * 예: {"tempo": 120.5, "energy": 0.7, "danceability": 0.8, "valence": 0.6}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audio_features", columnDefinition = "jsonb")
    private JsonNode audioFeatures;
    
    /**
     * 청취 패턴 (JSON)
     * 예: {"peak_hours": [18, 19, 20], "weekend_preference": 0.3}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "listening_patterns", columnDefinition = "jsonb")
    private JsonNode listeningPatterns;
    
    @LastModifiedDate
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}