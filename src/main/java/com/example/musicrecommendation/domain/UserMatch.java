package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 사용자 매칭 엔티티 (간소화 버전)
 */
@Entity
@Table(name = "user_matches")
public class UserMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    @Column(name = "similarity_score")
    private Double similarityScore;

    @Column(name = "match_status", length = 20)
    private String matchStatus = "PENDING";

    @Column(name = "match_reason", length = 500)
    private String matchReason;

    @Column(name = "common_liked_songs")
    private Integer commonLikedSongs = 0;

    @Column(name = "common_artists")
    private Integer commonArtists = 0;

    @Column(name = "common_genres", length = 500)
    private String commonGenres;

    @Column(name = "match_type", length = 20)
    private String matchType = "MUSIC_TASTE";

    @Column(name = "last_interaction_at")
    private LocalDateTime lastInteractionAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 기본 생성자
    public UserMatch() {}

    // 생성자
    public UserMatch(Long user1Id, Long user2Id, Double similarityScore, String matchReason) {
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.similarityScore = similarityScore;
        this.matchReason = matchReason;
        this.matchType = "MUSIC_TASTE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUser1Id() { return user1Id; }
    public void setUser1Id(Long user1Id) { this.user1Id = user1Id; }

    public Long getUser2Id() { return user2Id; }
    public void setUser2Id(Long user2Id) { this.user2Id = user2Id; }

    public Double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(Double similarityScore) { this.similarityScore = similarityScore; }

    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }

    public String getMatchReason() { return matchReason; }
    public void setMatchReason(String matchReason) { this.matchReason = matchReason; }

    public Integer getCommonLikedSongs() { return commonLikedSongs; }
    public void setCommonLikedSongs(Integer commonLikedSongs) { this.commonLikedSongs = commonLikedSongs; }

    public Integer getCommonArtists() { return commonArtists; }
    public void setCommonArtists(Integer commonArtists) { this.commonArtists = commonArtists; }

    public String getCommonGenres() { return commonGenres; }
    public void setCommonGenres(String commonGenres) { this.commonGenres = commonGenres; }

    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }

    public LocalDateTime getLastInteractionAt() { return lastInteractionAt; }
    public void setLastInteractionAt(LocalDateTime lastInteractionAt) { this.lastInteractionAt = lastInteractionAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}