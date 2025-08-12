package com.example.musicrecommendation.web.dto;

import com.example.musicrecommendation.domain.UserSongLike;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 좋아요 정보 응답 DTO
 */
@Schema(description = "좋아요 정보 응답")
public class LikeResponse {

    @Schema(description = "좋아요 ID", example = "1")
    private Long id;

    @Schema(description = "사용자 정보")
    private UserResponse user;

    @Schema(description = "곡 정보")
    private SongResponse song;

    @Schema(description = "좋아요를 누른 시간")
    private LocalDateTime likedAt;

    /**
     * 기본 생성자
     */
    public LikeResponse() {}

    /**
     * 모든 필드를 받는 생성자
     */
    public LikeResponse(Long id, UserResponse user, SongResponse song, LocalDateTime likedAt) {
        this.id = id;
        this.user = user;
        this.song = song;
        this.likedAt = likedAt;
    }

    /**
     * UserSongLike 엔티티로부터 LikeResponse 생성하는 팩토리 메서드
     *
     * @param userSongLike UserSongLike 엔티티
     * @return LikeResponse 객체
     */
    public static LikeResponse from(UserSongLike userSongLike) {
        return new LikeResponse(
                userSongLike.getId(),
                UserResponse.from(userSongLike.getUser()),
                SongResponse.from(userSongLike.getSong()),
                userSongLike.getLikedAt()
        );
    }

    // === Getters and Setters ===
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public SongResponse getSong() {
        return song;
    }

    public void setSong(SongResponse song) {
        this.song = song;
    }

    public LocalDateTime getLikedAt() {
        return likedAt;
    }

    public void setLikedAt(LocalDateTime likedAt) {
        this.likedAt = likedAt;
    }
}