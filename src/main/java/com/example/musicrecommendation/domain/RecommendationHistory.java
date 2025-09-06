package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_history")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "track_id", nullable = false, length = 100)
    private String trackId;
    
    @Column(name = "track_title", nullable = false, length = 500)
    private String trackTitle;
    
    @Column(name = "track_artist", nullable = false, length = 500)
    private String trackArtist;
    
    @Column(name = "track_genre", length = 100)
    private String trackGenre;
    
    @Column(name = "recommendation_type", length = 100)
    private String recommendationType; // "리뷰 기반", "선호 아티스트", "선호 장르", "트렌딩" 등
    
    @Column(name = "recommendation_score")
    private Integer recommendationScore;
    
    @Column(name = "spotify_id", length = 100)
    private String spotifyId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "session_id", length = 100)
    private String sessionId; // 같은 세션에서 생성된 추천들을 그룹화하기 위함
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}