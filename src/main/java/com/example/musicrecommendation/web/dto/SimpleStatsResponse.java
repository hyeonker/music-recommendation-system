package com.example.musicrecommendation.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 통계 응답 DTO
 */
@Schema(description = "통계 응답")
public class SimpleStatsResponse {

    @Schema(description = "곡 정보")
    public static class SongResponse {
        @Schema(description = "곡 ID")
        private Long id;

        @Schema(description = "곡 제목")
        private String title;

        @Schema(description = "아티스트")
        private String artist;

        @Schema(description = "앨범")
        private String album;

        @Schema(description = "좋아요 수")
        private Long likeCount;

        public SongResponse() {}

        public SongResponse(Long id, String title, String artist, String album, Long likeCount) {
            this.id = id;
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.likeCount = likeCount;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }

        public String getAlbum() { return album; }
        public void setAlbum(String album) { this.album = album; }

        public Long getLikeCount() { return likeCount; }
        public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
    }

    @Schema(description = "TOP 곡 정보")
    public static class TopSongDto {
        @Schema(description = "순위")
        private Integer rank;

        @Schema(description = "곡 ID")
        private Long songId;

        @Schema(description = "곡 제목")
        private String title;

        @Schema(description = "아티스트")
        private String artist;

        @Schema(description = "앨범")
        private String album;

        @Schema(description = "좋아요 수")
        private Long likeCount;

        @Schema(description = "점유율 (%)")
        private Double percentage;

        public TopSongDto() {}

        public TopSongDto(Integer rank, Long songId, String title, String artist, String album, Long likeCount, Double percentage) {
            this.rank = rank;
            this.songId = songId;
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.likeCount = likeCount;
            this.percentage = percentage;
        }

        // Getters and Setters
        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }

        public Long getSongId() { return songId; }
        public void setSongId(Long songId) { this.songId = songId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }

        public String getAlbum() { return album; }
        public void setAlbum(String album) { this.album = album; }

        public Long getLikeCount() { return likeCount; }
        public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }

        public Double getPercentage() { return percentage; }
        public void setPercentage(Double percentage) { this.percentage = percentage; }
    }

    @Schema(description = "TOP 사용자 정보")
    public static class TopUserDto {
        @Schema(description = "순위")
        private Integer rank;

        @Schema(description = "사용자 ID")
        private Long userId;

        @Schema(description = "사용자명")
        private String username;

        @Schema(description = "이메일")
        private String email;

        @Schema(description = "총 좋아요 수")
        private Long totalLikes;

        @Schema(description = "활동 점수")
        private Double activityScore;

        public TopUserDto() {}

        public TopUserDto(Integer rank, Long userId, String username, String email, Long totalLikes, Double activityScore) {
            this.rank = rank;
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.totalLikes = totalLikes;
            this.activityScore = activityScore;
        }

        // Getters and Setters
        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public Long getTotalLikes() { return totalLikes; }
        public void setTotalLikes(Long totalLikes) { this.totalLikes = totalLikes; }

        public Double getActivityScore() { return activityScore; }
        public void setActivityScore(Double activityScore) { this.activityScore = activityScore; }
    }

    @Schema(description = "TOP 아티스트 정보")
    public static class TopArtistDto {
        @Schema(description = "순위")
        private Integer rank;

        @Schema(description = "아티스트명")
        private String name;

        @Schema(description = "총 좋아요 수")
        private Long totalLikes;

        @Schema(description = "곡 수")
        private Long songCount;

        @Schema(description = "인기도")
        private Double popularity;

        public TopArtistDto() {}

        public TopArtistDto(Integer rank, String name, Long totalLikes, Long songCount, Double popularity) {
            this.rank = rank;
            this.name = name;
            this.totalLikes = totalLikes;
            this.songCount = songCount;
            this.popularity = popularity;
        }

        // Getters and Setters
        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Long getTotalLikes() { return totalLikes; }
        public void setTotalLikes(Long totalLikes) { this.totalLikes = totalLikes; }

        public Long getSongCount() { return songCount; }
        public void setSongCount(Long songCount) { this.songCount = songCount; }

        public Double getPopularity() { return popularity; }
        public void setPopularity(Double popularity) { this.popularity = popularity; }
    }

    @Schema(description = "전체 통계 정보")
    public static class OverallStatsDto {
        @Schema(description = "총 사용자 수")
        private Long totalUsers;

        @Schema(description = "총 곡 수")
        private Long totalSongs;

        @Schema(description = "총 좋아요 수")
        private Long totalLikes;

        @Schema(description = "평균 사용자 활동")
        private Double averageUserActivity;

        @Schema(description = "가장 인기 있는 장르")
        private String topGenre;

        @Schema(description = "응답 시간")
        private LocalDateTime timestamp;

        public OverallStatsDto() {}

        public OverallStatsDto(Long totalUsers, Long totalSongs, Long totalLikes, Double averageUserActivity, String topGenre) {
            this.totalUsers = totalUsers;
            this.totalSongs = totalSongs;
            this.totalLikes = totalLikes;
            this.averageUserActivity = averageUserActivity;
            this.topGenre = topGenre;
            this.timestamp = LocalDateTime.now();
        }

        // Getters and Setters
        public Long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }

        public Long getTotalSongs() { return totalSongs; }
        public void setTotalSongs(Long totalSongs) { this.totalSongs = totalSongs; }

        public Long getTotalLikes() { return totalLikes; }
        public void setTotalLikes(Long totalLikes) { this.totalLikes = totalLikes; }

        public Double getAverageUserActivity() { return averageUserActivity; }
        public void setAverageUserActivity(Double averageUserActivity) { this.averageUserActivity = averageUserActivity; }

        public String getTopGenre() { return topGenre; }
        public void setTopGenre(String topGenre) { this.topGenre = topGenre; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    @Schema(description = "사용자 정보")
    public static class UserDto {
        @Schema(description = "사용자 ID")
        private Long id;

        @Schema(description = "사용자명")
        private String username;

        @Schema(description = "이메일")
        private String email;

        @Schema(description = "총 좋아요 수")
        private Long totalLikes;

        @Schema(description = "활동 점수")
        private Double activityScore;

        public UserDto() {}

        public UserDto(Long id, String username, String email, Long totalLikes, Double activityScore) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.totalLikes = totalLikes;
            this.activityScore = activityScore;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public Long getTotalLikes() { return totalLikes; }
        public void setTotalLikes(Long totalLikes) { this.totalLikes = totalLikes; }

        public Double getActivityScore() { return activityScore; }
        public void setActivityScore(Double activityScore) { this.activityScore = activityScore; }
    }

    @Schema(description = "아티스트 정보")
    public static class ArtistDto {
        @Schema(description = "순위")
        private Integer rank;

        @Schema(description = "아티스트명")
        private String name;

        @Schema(description = "총 좋아요 수")
        private Long totalLikes;

        @Schema(description = "곡 수")
        private Long songCount;

        @Schema(description = "인기도")
        private Double popularity;

        public ArtistDto() {}

        public ArtistDto(Integer rank, String name, Long totalLikes, Long songCount, Double popularity) {
            this.rank = rank;
            this.name = name;
            this.totalLikes = totalLikes;
            this.songCount = songCount;
            this.popularity = popularity;
        }

        // Getters and Setters
        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Long getTotalLikes() { return totalLikes; }
        public void setTotalLikes(Long totalLikes) { this.totalLikes = totalLikes; }

        public Long getSongCount() { return songCount; }
        public void setSongCount(Long songCount) { this.songCount = songCount; }

        public Double getPopularity() { return popularity; }
        public void setPopularity(Double popularity) { this.popularity = popularity; }
    }

    @Schema(description = "대시보드 응답")
    public static class DashboardResponse {
        @Schema(description = "총 사용자 수")
        private Long totalUsers;

        @Schema(description = "총 곡 수")
        private Long totalSongs;

        @Schema(description = "총 좋아요 수")
        private Long totalLikes;

        @Schema(description = "활발한 사용자들")
        private List<UserDto> activeUsers;

        @Schema(description = "인기 곡들")
        private List<TopSongDto> popularSongs;

        @Schema(description = "인기 아티스트들")
        private List<ArtistDto> popularArtists;

        @Schema(description = "응답 시간")
        private LocalDateTime timestamp;

        public DashboardResponse() {}

        public DashboardResponse(Long totalUsers, Long totalSongs, Long totalLikes,
                                 List<UserDto> activeUsers, List<TopSongDto> popularSongs,
                                 List<ArtistDto> popularArtists) {
            this.totalUsers = totalUsers;
            this.totalSongs = totalSongs;
            this.totalLikes = totalLikes;
            this.activeUsers = activeUsers;
            this.popularSongs = popularSongs;
            this.popularArtists = popularArtists;
            this.timestamp = LocalDateTime.now();
        }

        // Getters and Setters
        public Long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }

        public Long getTotalSongs() { return totalSongs; }
        public void setTotalSongs(Long totalSongs) { this.totalSongs = totalSongs; }

        public Long getTotalLikes() { return totalLikes; }
        public void setTotalLikes(Long totalLikes) { this.totalLikes = totalLikes; }

        public List<UserDto> getActiveUsers() { return activeUsers; }
        public void setActiveUsers(List<UserDto> activeUsers) { this.activeUsers = activeUsers; }

        public List<TopSongDto> getPopularSongs() { return popularSongs; }
        public void setPopularSongs(List<TopSongDto> popularSongs) { this.popularSongs = popularSongs; }

        public List<ArtistDto> getPopularArtists() { return popularArtists; }
        public void setPopularArtists(List<ArtistDto> popularArtists) { this.popularArtists = popularArtists; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}