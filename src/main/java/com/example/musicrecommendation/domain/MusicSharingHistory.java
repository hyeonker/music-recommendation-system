package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "music_sharing_history")
public class MusicSharingHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "shared_by_user_id", nullable = false)
    private Long sharedByUserId;
    
    @Column(name = "shared_by_name", nullable = false, length = 100)
    private String sharedByName;
    
    @Column(name = "track_name", nullable = false, length = 500)
    private String trackName;
    
    @Column(name = "artist_name", nullable = false, length = 300)
    private String artistName;
    
    @Column(name = "spotify_url", columnDefinition = "TEXT")
    private String spotifyUrl;
    
    @Column(name = "album_name", length = 500)
    private String albumName;
    
    @Column(name = "album_image_url", columnDefinition = "TEXT")
    private String albumImageUrl;
    
    @Column(name = "preview_url", columnDefinition = "TEXT")
    private String previewUrl;
    
    @Column(name = "duration_ms")
    private Integer durationMs;
    
    @Column(name = "popularity")
    private Integer popularity;
    
    @Column(name = "genres", columnDefinition = "TEXT[]")
    private String[] genres;
    
    @Column(name = "shared_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT NOW()")
    private LocalDateTime sharedAt;
    
    @Column(name = "chat_room_id")
    private String chatRoomId;
    
    @Column(name = "matching_session_id")
    private String matchingSessionId;
    
    @Column(name = "is_liked", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isLiked = false;
    
    @Column(name = "liked_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private LocalDateTime likedAt;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    // 기본 생성자
    public MusicSharingHistory() {}
    
    // 생성자
    public MusicSharingHistory(Long userId, Long sharedByUserId, String sharedByName, 
                              String trackName, String artistName, String spotifyUrl) {
        this.userId = userId;
        this.sharedByUserId = sharedByUserId;
        this.sharedByName = sharedByName;
        this.trackName = trackName;
        this.artistName = artistName;
        this.spotifyUrl = spotifyUrl;
        this.sharedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getSharedByUserId() {
        return sharedByUserId;
    }
    
    public void setSharedByUserId(Long sharedByUserId) {
        this.sharedByUserId = sharedByUserId;
    }
    
    public String getSharedByName() {
        return sharedByName;
    }
    
    public void setSharedByName(String sharedByName) {
        this.sharedByName = sharedByName;
    }
    
    public String getTrackName() {
        return trackName;
    }
    
    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }
    
    public String getArtistName() {
        return artistName;
    }
    
    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }
    
    public String getSpotifyUrl() {
        return spotifyUrl;
    }
    
    public void setSpotifyUrl(String spotifyUrl) {
        this.spotifyUrl = spotifyUrl;
    }
    
    public String getAlbumName() {
        return albumName;
    }
    
    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }
    
    public String getAlbumImageUrl() {
        return albumImageUrl;
    }
    
    public void setAlbumImageUrl(String albumImageUrl) {
        this.albumImageUrl = albumImageUrl;
    }
    
    public String getPreviewUrl() {
        return previewUrl;
    }
    
    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }
    
    public Integer getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }
    
    public Integer getPopularity() {
        return popularity;
    }
    
    public void setPopularity(Integer popularity) {
        this.popularity = popularity;
    }
    
    public String[] getGenres() {
        return genres;
    }
    
    public void setGenres(String[] genres) {
        this.genres = genres;
    }
    
    public LocalDateTime getSharedAt() {
        return sharedAt;
    }
    
    public void setSharedAt(LocalDateTime sharedAt) {
        this.sharedAt = sharedAt;
    }
    
    public String getChatRoomId() {
        return chatRoomId;
    }
    
    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }
    
    public String getMatchingSessionId() {
        return matchingSessionId;
    }
    
    public void setMatchingSessionId(String matchingSessionId) {
        this.matchingSessionId = matchingSessionId;
    }
    
    public Boolean getIsLiked() {
        return isLiked;
    }
    
    public void setIsLiked(Boolean isLiked) {
        this.isLiked = isLiked;
        if (isLiked && this.likedAt == null) {
            this.likedAt = LocalDateTime.now();
        } else if (!isLiked) {
            this.likedAt = null;
        }
    }
    
    public LocalDateTime getLikedAt() {
        return likedAt;
    }
    
    public void setLikedAt(LocalDateTime likedAt) {
        this.likedAt = likedAt;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    @PrePersist
    public void prePersist() {
        if (this.sharedAt == null) {
            this.sharedAt = LocalDateTime.now();
        }
        if (this.isLiked == null) {
            this.isLiked = false;
        }
    }
}