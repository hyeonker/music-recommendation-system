package com.example.musicrecommendation.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 추천 시스템 응답 DTO
 */
@Schema(description = "음악 추천 응답")
public class RecommendationResponse {

    @Schema(description = "추천 데이터 (고도화 버전)")
    public static class RecommendationData {
        @Schema(description = "추천 제목")
        private String title;

        @Schema(description = "추천 설명")
        private String description;

        @Schema(description = "추천 곡 목록")
        private List<RecommendedSongDto> recommendations;

        @Schema(description = "생성 시간")
        private LocalDateTime timestamp;

        public RecommendationData() {}

        public RecommendationData(String title, String description, List<RecommendedSongDto> recommendations, LocalDateTime timestamp) {
            this.title = title;
            this.description = description;
            this.recommendations = recommendations;
            this.timestamp = timestamp;
        }

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<RecommendedSongDto> getRecommendations() { return recommendations; }
        public void setRecommendations(List<RecommendedSongDto> recommendations) { this.recommendations = recommendations; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    @Schema(description = "추천 곡 정보")
    public static class RecommendedSongDto {
        @Schema(description = "순위")
        private Integer rank;

        @Schema(description = "곡 정보")
        private SimpleStatsResponse.SongResponse song;

        @Schema(description = "추천 점수 (0.0 ~ 1.0)")
        private Double score;

        @Schema(description = "추천 이유")
        private String reason;

        @Schema(description = "태그")
        private String tag;

        public RecommendedSongDto() {}

        public RecommendedSongDto(Integer rank, SimpleStatsResponse.SongResponse song, Double score, String reason, String tag) {
            this.rank = rank;
            this.song = song;
            this.score = score;
            this.reason = reason;
            this.tag = tag;
        }

        // Getters and Setters
        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }

        public SimpleStatsResponse.SongResponse getSong() { return song; }
        public void setSong(SimpleStatsResponse.SongResponse song) { this.song = song; }

        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
    }
}