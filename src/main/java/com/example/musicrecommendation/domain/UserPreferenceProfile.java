package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 사용자 음악 취향 프로필 엔티티
 */
@Entity
@Table(name = "user_preference_profiles")
public class UserPreferenceProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // === 🎵 평균 음악 특성 (사용자가 좋아하는 음악들의 평균값) ===

    @Column(name = "avg_acousticness")
    private Float avgAcousticness;

    @Column(name = "avg_danceability")
    private Float avgDanceability;

    @Column(name = "avg_energy")
    private Float avgEnergy;

    @Column(name = "avg_instrumentalness")
    private Float avgInstrumentalness;

    @Column(name = "avg_liveness")
    private Float avgLiveness;

    @Column(name = "avg_speechiness")
    private Float avgSpeechiness;

    @Column(name = "avg_valence")
    private Float avgValence;

    @Column(name = "avg_tempo")
    private Float avgTempo;

    // === 🎭 선호 장르 및 패턴 ===

    @Column(name = "preferred_genres", length = 1000)
    private String preferredGenres; // JSON 배열 형태

    @Column(name = "preferred_artists", length = 1000)
    private String preferredArtists; // JSON 배열 형태

    @Column(name = "listening_diversity_score")
    private Float listeningDiversityScore; // 0.0~1.0 (다양성 점수)

    // === 📊 활동 패턴 ===

    @Column(name = "total_likes")
    private Integer totalLikes;

    @Column(name = "avg_likes_per_day")
    private Float avgLikesPerDay;

    @Column(name = "most_active_hour")
    private Integer mostActiveHour; // 0-23 시간

    @Column(name = "most_active_day")
    private Integer mostActiveDay; // 1-7 (월요일=1)

    // === 🎯 추천 가중치 ===

    @Column(name = "popularity_weight")
    private Float popularityWeight; // 인기곡 선호도

    @Column(name = "discovery_weight")
    private Float discoveryWeight; // 신곡 발견 선호도

    @Column(name = "similarity_weight")
    private Float similarityWeight; // 유사한 곡 선호도

    // === 📅 메타데이터 ===

    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // === 생성자 ===

    public UserPreferenceProfile() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // 기본 가중치 설정
        this.popularityWeight = 0.3f;
        this.discoveryWeight = 0.3f;
        this.similarityWeight = 0.4f;
    }

    public UserPreferenceProfile(User user) {
        this();
        this.user = user;
    }

    // === 🎯 유틸리티 메서드들 ===

    /**
     * 사용자 취향 벡터 생성 (추천 알고리즘용)
     */
    public double[] getPreferenceVector() {
        return new double[] {
                avgAcousticness != null ? avgAcousticness : 0.5,
                avgDanceability != null ? avgDanceability : 0.5,
                avgEnergy != null ? avgEnergy : 0.5,
                avgInstrumentalness != null ? avgInstrumentalness : 0.5,
                avgLiveness != null ? avgLiveness : 0.5,
                avgSpeechiness != null ? avgSpeechiness : 0.5,
                avgValence != null ? avgValence : 0.5,
                avgTempo != null ? (avgTempo / 200.0) : 0.5 // 정규화
        };
    }

    /**
     * 취향 설명 생성
     */
    public String getPreferenceDescription() {
        StringBuilder desc = new StringBuilder();

        if (avgEnergy != null && avgEnergy > 0.7) desc.append("에너지 넘치는 ");
        if (avgDanceability != null && avgDanceability > 0.7) desc.append("댄서블한 ");
        if (avgAcousticness != null && avgAcousticness > 0.7) desc.append("어쿠스틱한 ");
        if (avgValence != null && avgValence > 0.7) desc.append("밝고 긍정적인 ");
        else if (avgValence != null && avgValence < 0.3) desc.append("감성적이고 차분한 ");

        if (desc.length() == 0) desc.append("다양한 장르의 ");
        desc.append("음악을 선호");

        return desc.toString();
    }

    /**
     * 프로필 업데이트 시간 갱신
     */
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
        this.lastCalculatedAt = LocalDateTime.now();
    }

    /**
     * 취향 다양성 점수 계산
     */
    public void calculateDiversityScore() {
        // 간단한 다양성 계산 (표준편차 기반)
        double[] preferences = getPreferenceVector();
        double mean = 0.5; // 중간값
        double variance = 0;

        for (double pref : preferences) {
            variance += Math.pow(pref - mean, 2);
        }
        variance /= preferences.length;

        this.listeningDiversityScore = (float) Math.sqrt(variance);
    }

    // === Getters and Setters ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Float getAvgAcousticness() { return avgAcousticness; }
    public void setAvgAcousticness(Float avgAcousticness) { this.avgAcousticness = avgAcousticness; }

    public Float getAvgDanceability() { return avgDanceability; }
    public void setAvgDanceability(Float avgDanceability) { this.avgDanceability = avgDanceability; }

    public Float getAvgEnergy() { return avgEnergy; }
    public void setAvgEnergy(Float avgEnergy) { this.avgEnergy = avgEnergy; }

    public Float getAvgInstrumentalness() { return avgInstrumentalness; }
    public void setAvgInstrumentalness(Float avgInstrumentalness) { this.avgInstrumentalness = avgInstrumentalness; }

    public Float getAvgLiveness() { return avgLiveness; }
    public void setAvgLiveness(Float avgLiveness) { this.avgLiveness = avgLiveness; }

    public Float getAvgSpeechiness() { return avgSpeechiness; }
    public void setAvgSpeechiness(Float avgSpeechiness) { this.avgSpeechiness = avgSpeechiness; }

    public Float getAvgValence() { return avgValence; }
    public void setAvgValence(Float avgValence) { this.avgValence = avgValence; }

    public Float getAvgTempo() { return avgTempo; }
    public void setAvgTempo(Float avgTempo) { this.avgTempo = avgTempo; }

    public String getPreferredGenres() { return preferredGenres; }
    public void setPreferredGenres(String preferredGenres) { this.preferredGenres = preferredGenres; }

    public String getPreferredArtists() { return preferredArtists; }
    public void setPreferredArtists(String preferredArtists) { this.preferredArtists = preferredArtists; }

    public Float getListeningDiversityScore() { return listeningDiversityScore; }
    public void setListeningDiversityScore(Float listeningDiversityScore) { this.listeningDiversityScore = listeningDiversityScore; }

    public Integer getTotalLikes() { return totalLikes; }
    public void setTotalLikes(Integer totalLikes) { this.totalLikes = totalLikes; }

    public Float getAvgLikesPerDay() { return avgLikesPerDay; }
    public void setAvgLikesPerDay(Float avgLikesPerDay) { this.avgLikesPerDay = avgLikesPerDay; }

    public Integer getMostActiveHour() { return mostActiveHour; }
    public void setMostActiveHour(Integer mostActiveHour) { this.mostActiveHour = mostActiveHour; }

    public Integer getMostActiveDay() { return mostActiveDay; }
    public void setMostActiveDay(Integer mostActiveDay) { this.mostActiveDay = mostActiveDay; }

    public Float getPopularityWeight() { return popularityWeight; }
    public void setPopularityWeight(Float popularityWeight) { this.popularityWeight = popularityWeight; }

    public Float getDiscoveryWeight() { return discoveryWeight; }
    public void setDiscoveryWeight(Float discoveryWeight) { this.discoveryWeight = discoveryWeight; }

    public Float getSimilarityWeight() { return similarityWeight; }
    public void setSimilarityWeight(Float similarityWeight) { this.similarityWeight = similarityWeight; }

    public LocalDateTime getLastCalculatedAt() { return lastCalculatedAt; }
    public void setLastCalculatedAt(LocalDateTime lastCalculatedAt) { this.lastCalculatedAt = lastCalculatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}