package com.example.musicrecommendation.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시간별 차트 응답 DTO
 */
@Schema(description = "시간별 차트")
public class PeriodChartResponse {

    @Schema(description = "차트 제목", example = "📅 8월 12일 일간 차트")
    private String chartTitle;

    @Schema(description = "차트 기간", example = "2025-08-12")
    private String period;

    @Schema(description = "차트 타입", example = "DAILY")
    private ChartType chartType;

    @Schema(description = "차트 곡 목록")
    private List<ChartSongDto> songs;

    @Schema(description = "차트 요약")
    private ChartSummary summary;

    @Schema(description = "업데이트 시간")
    private LocalDateTime updatedAt;

    // 생성자
    public PeriodChartResponse() {}

    public PeriodChartResponse(String chartTitle, String period, ChartType chartType,
                               List<ChartSongDto> songs, ChartSummary summary) {
        this.chartTitle = chartTitle;
        this.period = period;
        this.chartType = chartType;
        this.songs = songs;
        this.summary = summary;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getChartTitle() { return chartTitle; }
    public void setChartTitle(String chartTitle) { this.chartTitle = chartTitle; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public ChartType getChartType() { return chartType; }
    public void setChartType(ChartType chartType) { this.chartType = chartType; }

    public List<ChartSongDto> getSongs() { return songs; }
    public void setSongs(List<ChartSongDto> songs) { this.songs = songs; }

    public ChartSummary getSummary() { return summary; }
    public void setSummary(ChartSummary summary) { this.summary = summary; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /**
     * 차트 타입 enum
     */
    public enum ChartType {
        DAILY("일간"),
        WEEKLY("주간"),
        MONTHLY("월간"),
        TRENDING("급상승");

        private final String displayName;

        ChartType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    /**
     * 차트 곡 정보 DTO
     */
    @Schema(description = "차트 곡 정보")
    public static class ChartSongDto {

        @Schema(description = "순위", example = "1")
        private int rank;

        @Schema(description = "곡 ID", example = "1")
        private Long songId;

        @Schema(description = "곡 제목", example = "Shape of You")
        private String title;

        @Schema(description = "아티스트", example = "Ed Sheeran")
        private String artist;

        @Schema(description = "좋아요 수", example = "42")
        private long likeCount;

        @Schema(description = "순위 변동", example = "UP")
        private RankChange rankChange;

        @Schema(description = "순위 변동 수치", example = "3")
        private int rankChangeValue;

        @Schema(description = "트렌드 아이콘", example = "🔥")
        private String trendIcon;

        @Schema(description = "뱃지", example = "🏆 1위")
        private String badge;

        @Schema(description = "차트 진입 여부", example = "true")
        private boolean isNew;

        // 생성자
        public ChartSongDto() {}

        public ChartSongDto(int rank, Long songId, String title, String artist,
                            long likeCount, RankChange rankChange, int rankChangeValue) {
            this.rank = rank;
            this.songId = songId;
            this.title = title;
            this.artist = artist;
            this.likeCount = likeCount;
            this.rankChange = rankChange;
            this.rankChangeValue = rankChangeValue;
            this.isNew = rankChange == RankChange.NEW;

            // 아이콘과 뱃지 설정
            setTrendIconAndBadge();
        }

        private void setTrendIconAndBadge() {
            // 순위별 뱃지
            switch (rank) {
                case 1: badge = "👑 1위"; trendIcon = "👑"; break;
                case 2: badge = "🥈 2위"; trendIcon = "🥈"; break;
                case 3: badge = "🥉 3위"; trendIcon = "🥉"; break;
                default:
                    if (rank <= 10) {
                        badge = "🏆 TOP " + rank;
                        trendIcon = "🏆";
                    } else {
                        badge = rank + "위";
                        trendIcon = "🎵";
                    }
            }

            // 순위 변동에 따른 아이콘 조정
            if (rankChange == RankChange.UP && rankChangeValue >= 5) {
                trendIcon = "🔥"; // 급상승
            } else if (rankChange == RankChange.NEW) {
                trendIcon = "⭐"; // 신규진입
                badge = "⭐ NEW " + rank + "위";
            }
        }

        // Getters and Setters
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }

        public Long getSongId() { return songId; }
        public void setSongId(Long songId) { this.songId = songId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }

        public long getLikeCount() { return likeCount; }
        public void setLikeCount(long likeCount) { this.likeCount = likeCount; }

        public RankChange getRankChange() { return rankChange; }
        public void setRankChange(RankChange rankChange) { this.rankChange = rankChange; }

        public int getRankChangeValue() { return rankChangeValue; }
        public void setRankChangeValue(int rankChangeValue) { this.rankChangeValue = rankChangeValue; }

        public String getTrendIcon() { return trendIcon; }
        public void setTrendIcon(String trendIcon) { this.trendIcon = trendIcon; }

        public String getBadge() { return badge; }
        public void setBadge(String badge) { this.badge = badge; }

        public boolean isNew() { return isNew; }
        public void setNew(boolean isNew) { this.isNew = isNew; }
    }

    /**
     * 순위 변동 enum
     */
    public enum RankChange {
        UP("📈", "상승"),
        DOWN("📉", "하락"),
        SAME("➡️", "유지"),
        NEW("⭐", "신규");

        private final String icon;
        private final String description;

        RankChange(String icon, String description) {
            this.icon = icon;
            this.description = description;
        }

        public String getIcon() { return icon; }
        public String getDescription() { return description; }
    }

    /**
     * 차트 요약 정보
     */
    @Schema(description = "차트 요약")
    public static class ChartSummary {

        @Schema(description = "총 곡 수", example = "10")
        private int totalSongs;

        @Schema(description = "총 좋아요 수", example = "156")
        private long totalLikes;

        @Schema(description = "신규 진입곡 수", example = "3")
        private int newSongs;

        @Schema(description = "상승곡 수", example = "6")
        private int upSongs;

        @Schema(description = "하락곡 수", example = "1")
        private int downSongs;

        @Schema(description = "1위 곡 정보", example = "Shape of You (42 좋아요)")
        private String topSongInfo;

        // 생성자
        public ChartSummary() {}

        public ChartSummary(int totalSongs, long totalLikes, int newSongs,
                            int upSongs, int downSongs, String topSongInfo) {
            this.totalSongs = totalSongs;
            this.totalLikes = totalLikes;
            this.newSongs = newSongs;
            this.upSongs = upSongs;
            this.downSongs = downSongs;
            this.topSongInfo = topSongInfo;
        }

        // Getters and Setters
        public int getTotalSongs() { return totalSongs; }
        public void setTotalSongs(int totalSongs) { this.totalSongs = totalSongs; }

        public long getTotalLikes() { return totalLikes; }
        public void setTotalLikes(long totalLikes) { this.totalLikes = totalLikes; }

        public int getNewSongs() { return newSongs; }
        public void setNewSongs(int newSongs) { this.newSongs = newSongs; }

        public int getUpSongs() { return upSongs; }
        public void setUpSongs(int upSongs) { this.upSongs = upSongs; }

        public int getDownSongs() { return downSongs; }
        public void setDownSongs(int downSongs) { this.downSongs = downSongs; }

        public String getTopSongInfo() { return topSongInfo; }
        public void setTopSongInfo(String topSongInfo) { this.topSongInfo = topSongInfo; }
    }
}