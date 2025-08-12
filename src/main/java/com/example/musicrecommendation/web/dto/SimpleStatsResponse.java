package com.example.musicrecommendation.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 간단한 통계 대시보드 응답 DTO
 */
@Schema(description = "통계 대시보드")
public class SimpleStatsResponse {

    @Schema(description = "TOP 인기 곡들")
    private List<TopSongDto> topSongs;

    @Schema(description = "활발한 사용자들")
    private List<TopUserDto> activeUsers;

    @Schema(description = "인기 아티스트들")
    private List<TopArtistDto> topArtists;

    @Schema(description = "전체 통계")
    private OverallStatsDto overallStats;

    /**
     * 기본 생성자
     */
    public SimpleStatsResponse() {}

    /**
     * 전체 생성자
     */
    public SimpleStatsResponse(List<TopSongDto> topSongs, List<TopUserDto> activeUsers,
                               List<TopArtistDto> topArtists, OverallStatsDto overallStats) {
        this.topSongs = topSongs;
        this.activeUsers = activeUsers;
        this.topArtists = topArtists;
        this.overallStats = overallStats;
    }

    // === Getters and Setters ===
    public List<TopSongDto> getTopSongs() {
        return topSongs;
    }

    public void setTopSongs(List<TopSongDto> topSongs) {
        this.topSongs = topSongs;
    }

    public List<TopUserDto> getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(List<TopUserDto> activeUsers) {
        this.activeUsers = activeUsers;
    }

    public List<TopArtistDto> getTopArtists() {
        return topArtists;
    }

    public void setTopArtists(List<TopArtistDto> topArtists) {
        this.topArtists = topArtists;
    }

    public OverallStatsDto getOverallStats() {
        return overallStats;
    }

    public void setOverallStats(OverallStatsDto overallStats) {
        this.overallStats = overallStats;
    }

    // === 내부 DTO 클래스들 ===

    /**
     * TOP 곡 정보
     */
    @Schema(description = "TOP 곡 정보")
    public static class TopSongDto {
        @Schema(description = "순위", example = "1")
        private int rank;

        @Schema(description = "곡 정보")
        private SongResponse song;

        @Schema(description = "좋아요 수", example = "42")
        private long likeCount;

        @Schema(description = "순위 상승/하락", example = "🔥 급상승")
        private String trendIcon;

        public TopSongDto() {}

        public TopSongDto(int rank, SongResponse song, long likeCount, String trendIcon) {
            this.rank = rank;
            this.song = song;
            this.likeCount = likeCount;
            this.trendIcon = trendIcon;
        }

        // Getters and Setters
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        public SongResponse getSong() { return song; }
        public void setSong(SongResponse song) { this.song = song; }
        public long getLikeCount() { return likeCount; }
        public void setLikeCount(long likeCount) { this.likeCount = likeCount; }
        public String getTrendIcon() { return trendIcon; }
        public void setTrendIcon(String trendIcon) { this.trendIcon = trendIcon; }
    }

    /**
     * TOP 사용자 정보
     */
    @Schema(description = "활발한 사용자 정보")
    public static class TopUserDto {
        @Schema(description = "순위", example = "1")
        private int rank;

        @Schema(description = "사용자 정보")
        private UserResponse user;

        @Schema(description = "좋아요 누른 수", example = "28")
        private long likeCount;

        @Schema(description = "활동 점수", example = "92.5")
        private double activityScore;

        @Schema(description = "뱃지", example = "🎵 음악광")
        private String badge;

        public TopUserDto() {}

        public TopUserDto(int rank, UserResponse user, long likeCount, double activityScore, String badge) {
            this.rank = rank;
            this.user = user;
            this.likeCount = likeCount;
            this.activityScore = activityScore;
            this.badge = badge;
        }

        // Getters and Setters
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        public UserResponse getUser() { return user; }
        public void setUser(UserResponse user) { this.user = user; }
        public long getLikeCount() { return likeCount; }
        public void setLikeCount(long likeCount) { this.likeCount = likeCount; }
        public double getActivityScore() { return activityScore; }
        public void setActivityScore(double activityScore) { this.activityScore = activityScore; }
        public String getBadge() { return badge; }
        public void setBadge(String badge) { this.badge = badge; }
    }

    /**
     * TOP 아티스트 정보
     */
    @Schema(description = "인기 아티스트 정보")
    public static class TopArtistDto {
        @Schema(description = "순위", example = "1")
        private int rank;

        @Schema(description = "아티스트명", example = "BTS")
        private String artistName;

        @Schema(description = "총 좋아요 수", example = "156")
        private long totalLikes;

        @Schema(description = "곡 수", example = "8")
        private int songCount;

        @Schema(description = "평균 좋아요", example = "19.5")
        private double averageLikes;

        @Schema(description = "대표곡")
        private SongResponse representativeSong;

        public TopArtistDto() {}

        public TopArtistDto(int rank, String artistName, long totalLikes, int songCount,
                            double averageLikes, SongResponse representativeSong) {
            this.rank = rank;
            this.artistName = artistName;
            this.totalLikes = totalLikes;
            this.songCount = songCount;
            this.averageLikes = averageLikes;
            this.representativeSong = representativeSong;
        }

        // Getters and Setters
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        public String getArtistName() { return artistName; }
        public void setArtistName(String artistName) { this.artistName = artistName; }
        public long getTotalLikes() { return totalLikes; }
        public void setTotalLikes(long totalLikes) { this.totalLikes = totalLikes; }
        public int getSongCount() { return songCount; }
        public void setSongCount(int songCount) { this.songCount = songCount; }
        public double getAverageLikes() { return averageLikes; }
        public void setAverageLikes(double averageLikes) { this.averageLikes = averageLikes; }
        public SongResponse getRepresentativeSong() { return representativeSong; }
        public void setRepresentativeSong(SongResponse representativeSong) { this.representativeSong = representativeSong; }
    }

    /**
     * 전체 통계 정보
     */
    @Schema(description = "전체 통계 정보")
    public static class OverallStatsDto {
        @Schema(description = "총 사용자 수", example = "42")
        private long totalUsers;

        @Schema(description = "총 곡 수", example = "128")
        private long totalSongs;

        @Schema(description = "총 좋아요 수", example = "856")
        private long totalLikes;

        @Schema(description = "오늘 새 좋아요", example = "23")
        private long todayLikes;

        @Schema(description = "평균 곡당 좋아요", example = "6.7")
        private double averageLikesPerSong;

        @Schema(description = "평균 사용자당 좋아요", example = "20.4")
        private double averageLikesPerUser;

        public OverallStatsDto() {}

        public OverallStatsDto(long totalUsers, long totalSongs, long totalLikes,
                               long todayLikes, double averageLikesPerSong, double averageLikesPerUser) {
            this.totalUsers = totalUsers;
            this.totalSongs = totalSongs;
            this.totalLikes = totalLikes;
            this.todayLikes = todayLikes;
            this.averageLikesPerSong = averageLikesPerSong;
            this.averageLikesPerUser = averageLikesPerUser;
        }

        // Getters and Setters
        public long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
        public long getTotalSongs() { return totalSongs; }
        public void setTotalSongs(long totalSongs) { this.totalSongs = totalSongs; }
        public long getTotalLikes() { return totalLikes; }
        public void setTotalLikes(long totalLikes) { this.totalLikes = totalLikes; }
        public long getTodayLikes() { return todayLikes; }
        public void setTodayLikes(long todayLikes) { this.todayLikes = todayLikes; }
        public double getAverageLikesPerSong() { return averageLikesPerSong; }
        public void setAverageLikesPerSong(double averageLikesPerSong) { this.averageLikesPerSong = averageLikesPerSong; }
        public double getAverageLikesPerUser() { return averageLikesPerUser; }
        public void setAverageLikesPerUser(double averageLikesPerUser) { this.averageLikesPerUser = averageLikesPerUser; }
    }
}