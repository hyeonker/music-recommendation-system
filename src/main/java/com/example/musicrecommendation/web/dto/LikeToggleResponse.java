package com.example.musicrecommendation.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 좋아요 토글 결과 응답 DTO
 */
@Schema(description = "좋아요 토글 결과")
public class LikeToggleResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "곡 ID", example = "1")
    private Long songId;

    @Schema(description = "좋아요 상태 (true: 추가됨, false: 취소됨)", example = "true")
    private boolean liked;

    @Schema(description = "해당 곡의 총 좋아요 수", example = "15")
    private long totalLikes;

    /**
     * 기본 생성자
     */
    public LikeToggleResponse() {}

    /**
     * 모든 필드를 받는 생성자
     */
    public LikeToggleResponse(Long userId, Long songId, boolean liked, long totalLikes) {
        this.userId = userId;
        this.songId = songId;
        this.liked = liked;
        this.totalLikes = totalLikes;
    }

    // === Getters and Setters ===
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSongId() {
        return songId;
    }

    public void setSongId(Long songId) {
        this.songId = songId;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public long getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(long totalLikes) {
        this.totalLikes = totalLikes;
    }
}