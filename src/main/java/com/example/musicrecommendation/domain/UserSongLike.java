package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 사용자가 곡에 누른 좋아요 정보를 저장하는 엔티티
 */
@Entity
@Table(name = "user_song_likes")
@EntityListeners(AuditingEntityListener.class)
public class UserSongLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 좋아요를 누른 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 좋아요를 받은 곡
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    /**
     * 좋아요를 누른 시간
     */
    @CreatedDate
    @Column(name = "liked_at")
    private LocalDateTime likedAt;

    /**
     * JPA 기본 생성자
     */
    protected UserSongLike() {}

    /**
     * 좋아요 생성자
     *
     * @param user 사용자
     * @param song 곡
     */
    public UserSongLike(User user, Song song) {
        this.user = user;
        this.song = song;
    }

    // === Getters ===
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Song getSong() {
        return song;
    }

    public LocalDateTime getLikedAt() {
        return likedAt;
    }

    // === 비즈니스 메서드 ===

    /**
     * 같은 사용자와 곡인지 확인
     *
     * @param userId 사용자 ID
     * @param songId 곡 ID
     * @return 같으면 true
     */
    public boolean isSameUserAndSong(Long userId, Long songId) {
        return this.user.getId().equals(userId) && this.song.getId().equals(songId);
    }

    @Override
    public String toString() {
        return "UserSongLike{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", songId=" + (song != null ? song.getId() : null) +
                ", likedAt=" + likedAt +
                '}';
    }
}