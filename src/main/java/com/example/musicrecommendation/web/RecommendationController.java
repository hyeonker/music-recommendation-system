package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.RecommendationService;
import com.example.musicrecommendation.service.UserPreferenceService;
import com.example.musicrecommendation.web.dto.RecommendationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 🎵 추천 시스템 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/recommendations")
@Tag(name = "🎵 추천 시스템 API", description = "개인화된 음악 추천 및 취향 분석")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserPreferenceService userPreferenceService;

    public RecommendationController(RecommendationService recommendationService,
                                    UserPreferenceService userPreferenceService) {
        this.recommendationService = recommendationService;
        this.userPreferenceService = userPreferenceService;
    }

    /**
     * 🎯 개인화된 추천 (메인 추천)
     */
    @GetMapping("/personalized/{userId}")
    @Operation(summary = "🎯 개인화된 음악 추천",
            description = "사용자의 취향을 분석하여 맞춤형 음악을 추천합니다.")
    @ApiResponse(responseCode = "200", description = "추천 성공")
    public ResponseEntity<RecommendationResponse> getPersonalizedRecommendations(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId,

            @Parameter(description = "추천받을 곡 수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        RecommendationResponse recommendations = recommendationService.getPersonalizedRecommendations(userId, limit);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * 👥 비슷한 취향 사용자 기반 추천
     */
    @GetMapping("/similar-users/{userId}")
    @Operation(summary = "👥 비슷한 취향 사용자 기반 추천",
            description = "비슷한 음악 취향을 가진 사용자들이 좋아하는 곡을 추천합니다.")
    @ApiResponse(responseCode = "200", description = "추천 성공")
    public ResponseEntity<RecommendationResponse> getSimilarUsersRecommendations(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId,

            @Parameter(description = "추천받을 곡 수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        RecommendationResponse recommendations = recommendationService.getSimilarUsersRecommendations(userId, limit);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * 🎵 특정 곡과 비슷한 곡 추천
     */
    @GetMapping("/similar-songs/{songId}")
    @Operation(summary = "🎵 비슷한 곡 추천",
            description = "선택한 곡과 유사한 특성을 가진 곡들을 추천합니다.")
    @ApiResponse(responseCode = "200", description = "추천 성공")
    public ResponseEntity<RecommendationResponse> getSimilarSongRecommendations(
            @Parameter(description = "기준 곡 ID", example = "1")
            @PathVariable Long songId,

            @Parameter(description = "추천받을 곡 수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        RecommendationResponse recommendations = recommendationService.getSimilarSongRecommendations(songId, limit);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * 🔥 인기곡 추천
     */
    @GetMapping("/popular")
    @Operation(summary = "🔥 인기곡 추천",
            description = "현재 가장 인기있는 곡들을 추천합니다.")
    @ApiResponse(responseCode = "200", description = "추천 성공")
    public ResponseEntity<RecommendationResponse> getPopularRecommendations(
            @Parameter(description = "추천받을 곡 수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        RecommendationResponse recommendations = recommendationService.getPopularRecommendations(limit, null);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * 💎 새로운 발견 추천
     */
    @GetMapping("/discovery/{userId}")
    @Operation(summary = "💎 새로운 발견 추천",
            description = "아직 널리 알려지지 않았지만 취향에 맞는 숨겨진 보석 같은 곡들을 추천합니다.")
    @ApiResponse(responseCode = "200", description = "추천 성공")
    public ResponseEntity<RecommendationResponse> getDiscoveryRecommendations(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId,

            @Parameter(description = "추천받을 곡 수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        RecommendationResponse recommendations = recommendationService.getDiscoveryRecommendations(userId, limit);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * 🌟 종합 추천 (다양한 알고리즘 혼합)
     */
    @GetMapping("/mixed/{userId}")
    @Operation(summary = "🌟 종합 추천",
            description = "개인 취향, 비슷한 사용자, 새로운 발견을 종합한 다양한 추천을 제공합니다.")
    @ApiResponse(responseCode = "200", description = "추천 성공")
    public ResponseEntity<RecommendationResponse> getMixedRecommendations(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId,

            @Parameter(description = "추천받을 곡 수", example = "15")
            @RequestParam(defaultValue = "15") int limit) {

        RecommendationResponse recommendations = recommendationService.getMixedRecommendations(userId, limit);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * 🎭 사용자 취향 분석
     */
    @GetMapping("/taste-profile/{userId}")
    @Operation(summary = "🎭 사용자 취향 분석",
            description = "사용자의 음악 취향을 분석하고 프로필을 제공합니다.")
    @ApiResponse(responseCode = "200", description = "분석 성공")
    public ResponseEntity<UserTasteAnalysisResponse> getUserTasteProfile(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId) {

        // 사용자 취향 프로필 생성/업데이트
        var userProfile = userPreferenceService.createOrUpdateUserProfile(userId);
        var tasteSummary = userPreferenceService.generateTasteSummary(userProfile);

        // DTO 생성
        UserTasteAnalysisResponse response = new UserTasteAnalysisResponse(
                userId,
                tasteSummary.mainCharacteristic,
                tasteSummary.description,
                tasteSummary.tags,
                userProfile.getListeningDiversityScore() != null ? userProfile.getListeningDiversityScore() : 0.5f,
                userProfile.getTotalLikes() != null ? userProfile.getTotalLikes() : 0,
                generateDetailedAnalysis(userProfile)
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 🎯 오늘의 추천 (일일 맞춤 추천)
     */
    @GetMapping("/daily/{userId}")
    @Operation(summary = "🎯 오늘의 추천",
            description = "매일 새로운 맞춤형 추천을 제공합니다.")
    @ApiResponse(responseCode = "200", description = "추천 성공")
    public ResponseEntity<RecommendationResponse> getDailyRecommendations(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId) {

        // 오늘의 추천은 종합 추천의 변형
        RecommendationResponse recommendations = recommendationService.getMixedRecommendations(userId, 12);

        // 제목을 오늘의 추천으로 변경
        recommendations.setTitle("오늘의 추천 플레이리스트");
        recommendations.setReason("매일 새롭게 큐레이션된 " + recommendations.getReason());

        return ResponseEntity.ok(recommendations);
    }

    // === Helper Methods ===

    private UserTasteAnalysisResponse.DetailedAnalysis generateDetailedAnalysis(
            com.example.musicrecommendation.domain.UserPreferenceProfile userProfile) {

        return new UserTasteAnalysisResponse.DetailedAnalysis(
                userProfile.getAvgEnergy() != null ? userProfile.getAvgEnergy() : 0.5f,
                userProfile.getAvgDanceability() != null ? userProfile.getAvgDanceability() : 0.5f,
                userProfile.getAvgValence() != null ? userProfile.getAvgValence() : 0.5f,
                userProfile.getAvgAcousticness() != null ? userProfile.getAvgAcousticness() : 0.5f,
                userProfile.getAvgTempo() != null ? userProfile.getAvgTempo() : 120.0f,
                userProfile.getPreferredGenres() != null ? userProfile.getPreferredGenres() : "",
                userProfile.getPreferredArtists() != null ? userProfile.getPreferredArtists() : ""
        );
    }

    /**
     * 사용자 취향 분석 응답 DTO
     */
    public static class UserTasteAnalysisResponse {
        private Long userId;
        private String mainCharacteristic;
        private String description;
        private java.util.List<String> tags;
        private float diversityScore;
        private int totalLikes;
        private DetailedAnalysis detailedAnalysis;

        public UserTasteAnalysisResponse() {}

        public UserTasteAnalysisResponse(Long userId, String mainCharacteristic, String description,
                                         java.util.List<String> tags, float diversityScore, int totalLikes,
                                         DetailedAnalysis detailedAnalysis) {
            this.userId = userId;
            this.mainCharacteristic = mainCharacteristic;
            this.description = description;
            this.tags = tags;
            this.diversityScore = diversityScore;
            this.totalLikes = totalLikes;
            this.detailedAnalysis = detailedAnalysis;
        }

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getMainCharacteristic() { return mainCharacteristic; }
        public void setMainCharacteristic(String mainCharacteristic) { this.mainCharacteristic = mainCharacteristic; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public java.util.List<String> getTags() { return tags; }
        public void setTags(java.util.List<String> tags) { this.tags = tags; }

        public float getDiversityScore() { return diversityScore; }
        public void setDiversityScore(float diversityScore) { this.diversityScore = diversityScore; }

        public int getTotalLikes() { return totalLikes; }
        public void setTotalLikes(int totalLikes) { this.totalLikes = totalLikes; }

        public DetailedAnalysis getDetailedAnalysis() { return detailedAnalysis; }
        public void setDetailedAnalysis(DetailedAnalysis detailedAnalysis) { this.detailedAnalysis = detailedAnalysis; }

        public static class DetailedAnalysis {
            private float avgEnergy;
            private float avgDanceability;
            private float avgValence;
            private float avgAcousticness;
            private float avgTempo;
            private String preferredGenres;
            private String preferredArtists;

            public DetailedAnalysis() {}

            public DetailedAnalysis(float avgEnergy, float avgDanceability, float avgValence,
                                    float avgAcousticness, float avgTempo, String preferredGenres,
                                    String preferredArtists) {
                this.avgEnergy = avgEnergy;
                this.avgDanceability = avgDanceability;
                this.avgValence = avgValence;
                this.avgAcousticness = avgAcousticness;
                this.avgTempo = avgTempo;
                this.preferredGenres = preferredGenres;
                this.preferredArtists = preferredArtists;
            }

            // Getters and Setters
            public float getAvgEnergy() { return avgEnergy; }
            public void setAvgEnergy(float avgEnergy) { this.avgEnergy = avgEnergy; }

            public float getAvgDanceability() { return avgDanceability; }
            public void setAvgDanceability(float avgDanceability) { this.avgDanceability = avgDanceability; }

            public float getAvgValence() { return avgValence; }
            public void setAvgValence(float avgValence) { this.avgValence = avgValence; }

            public float getAvgAcousticness() { return avgAcousticness; }
            public void setAvgAcousticness(float avgAcousticness) { this.avgAcousticness = avgAcousticness; }

            public float getAvgTempo() { return avgTempo; }
            public void setAvgTempo(float avgTempo) { this.avgTempo = avgTempo; }

            public String getPreferredGenres() { return preferredGenres; }
            public void setPreferredGenres(String preferredGenres) { this.preferredGenres = preferredGenres; }

            public String getPreferredArtists() { return preferredArtists; }
            public void setPreferredArtists(String preferredArtists) { this.preferredArtists = preferredArtists; }
        }
    }
}