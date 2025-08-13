package com.example.musicrecommendation.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 매칭 시스템 응답 DTO
 */
@Schema(description = "사용자 매칭 응답")
public class MatchingResponse {

    @Schema(description = "매칭 후보 응답")
    public static class MatchCandidateResponse {
        @Schema(description = "요청한 사용자 ID")
        private Long userId;

        @Schema(description = "매칭 후보들")
        private List<MatchDto> candidates;

        @Schema(description = "총 후보 수")
        private int totalCandidates;

        @Schema(description = "응답 시간")
        private LocalDateTime timestamp;

        public MatchCandidateResponse(Long userId, List<MatchDto> candidates) {
            this.userId = userId;
            this.candidates = candidates;
            this.totalCandidates = candidates.size();
            this.timestamp = LocalDateTime.now();
        }

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public List<MatchDto> getCandidates() { return candidates; }
        public void setCandidates(List<MatchDto> candidates) { this.candidates = candidates; }

        public int getTotalCandidates() { return totalCandidates; }
        public void setTotalCandidates(int totalCandidates) { this.totalCandidates = totalCandidates; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    @Schema(description = "매칭 정보")
    public static class MatchDto {
        @Schema(description = "매칭 ID")
        private Long matchId;

        @Schema(description = "상대방 사용자 정보")
        private UserInfo matchedUser;

        @Schema(description = "유사도 점수 (0.0 ~ 1.0)")
        private Double similarityScore;

        @Schema(description = "매칭 상태")
        private String matchStatus;

        @Schema(description = "매칭 이유")
        private String matchReason;

        @Schema(description = "공통 좋아요 곡 수")
        private Integer commonLikedSongs;

        @Schema(description = "공통 아티스트 수")
        private Integer commonArtists;

        @Schema(description = "생성 시간")
        private LocalDateTime createdAt;

        @Schema(description = "마지막 상호작용 시간")
        private LocalDateTime lastInteractionAt;

        public MatchDto() {}

        public MatchDto(Long matchId, UserInfo matchedUser, Double similarityScore,
                        String matchStatus, String matchReason, Integer commonLikedSongs) {
            this.matchId = matchId;
            this.matchedUser = matchedUser;
            this.similarityScore = similarityScore;
            this.matchStatus = matchStatus;
            this.matchReason = matchReason;
            this.commonLikedSongs = commonLikedSongs;
            this.createdAt = LocalDateTime.now();
        }

        // Getters and Setters
        public Long getMatchId() { return matchId; }
        public void setMatchId(Long matchId) { this.matchId = matchId; }

        public UserInfo getMatchedUser() { return matchedUser; }
        public void setMatchedUser(UserInfo matchedUser) { this.matchedUser = matchedUser; }

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

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getLastInteractionAt() { return lastInteractionAt; }
        public void setLastInteractionAt(LocalDateTime lastInteractionAt) { this.lastInteractionAt = lastInteractionAt; }
    }

    @Schema(description = "사용자 기본 정보")
    public static class UserInfo {
        @Schema(description = "사용자 ID")
        private Long id;

        @Schema(description = "사용자명")
        private String username;

        @Schema(description = "이메일")
        private String email;

        @Schema(description = "가입일")
        private LocalDateTime createdAt;

        public UserInfo() {}

        public UserInfo(Long id, String username, String email, LocalDateTime createdAt) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.createdAt = createdAt;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}