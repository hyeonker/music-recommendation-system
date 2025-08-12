package com.example.musicrecommendation.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 종합 차트 대시보드 응답 DTO
 */
@Schema(description = "종합 차트 대시보드")
public class ChartsDashboardResponse {

    @Schema(description = "📅 일간 차트")
    private PeriodChartResponse dailyChart;

    @Schema(description = "📊 주간 차트")
    private PeriodChartResponse weeklyChart;

    @Schema(description = "🏆 월간 차트")
    private PeriodChartResponse monthlyChart;

    @Schema(description = "🔥 급상승 차트")
    private PeriodChartResponse trendingChart;

    @Schema(description = "📈 차트 요약")
    private ChartSummary summary;

    // 생성자
    public ChartsDashboardResponse() {}

    public ChartsDashboardResponse(PeriodChartResponse dailyChart,
                                   PeriodChartResponse weeklyChart,
                                   PeriodChartResponse monthlyChart,
                                   PeriodChartResponse trendingChart,
                                   ChartSummary summary) {
        this.dailyChart = dailyChart;
        this.weeklyChart = weeklyChart;
        this.monthlyChart = monthlyChart;
        this.trendingChart = trendingChart;
        this.summary = summary;
    }

    // Getters and Setters
    public PeriodChartResponse getDailyChart() { return dailyChart; }
    public void setDailyChart(PeriodChartResponse dailyChart) { this.dailyChart = dailyChart; }

    public PeriodChartResponse getWeeklyChart() { return weeklyChart; }
    public void setWeeklyChart(PeriodChartResponse weeklyChart) { this.weeklyChart = weeklyChart; }

    public PeriodChartResponse getMonthlyChart() { return monthlyChart; }
    public void setMonthlyChart(PeriodChartResponse monthlyChart) { this.monthlyChart = monthlyChart; }

    public PeriodChartResponse getTrendingChart() { return trendingChart; }
    public void setTrendingChart(PeriodChartResponse trendingChart) { this.trendingChart = trendingChart; }

    public ChartSummary getSummary() { return summary; }
    public void setSummary(ChartSummary summary) { this.summary = summary; }

    /**
     * 차트 요약 정보
     */
    @Schema(description = "차트 요약")
    public static class ChartSummary {

        @Schema(description = "🏆 이번 달 1위", example = "Shape of You")
        private String monthlyChampion;

        @Schema(description = "📊 이번 주 1위", example = "Blinding Lights")
        private String weeklyChampion;

        @Schema(description = "📅 오늘 1위", example = "As It Was")
        private String dailyChampion;

        @Schema(description = "🔥 급상승 1위", example = "Anti-Hero")
        private String trendingChampion;

        @Schema(description = "📈 총 차트 진입곡", example = "25")
        private int totalChartsongs;

        @Schema(description = "🎵 활성 아티스트", example = "12")
        private int activeArtists;

        // 생성자
        public ChartSummary() {}

        public ChartSummary(String monthlyChampion, String weeklyChampion,
                            String dailyChampion, String trendingChampion,
                            int totalChartsongs, int activeArtists) {
            this.monthlyChampion = monthlyChampion;
            this.weeklyChampion = weeklyChampion;
            this.dailyChampion = dailyChampion;
            this.trendingChampion = trendingChampion;
            this.totalChartsongs = totalChartsongs;
            this.activeArtists = activeArtists;
        }

        // Getters and Setters
        public String getMonthlyChampion() { return monthlyChampion; }
        public void setMonthlyChampion(String monthlyChampion) { this.monthlyChampion = monthlyChampion; }

        public String getWeeklyChampion() { return weeklyChampion; }
        public void setWeeklyChampion(String weeklyChampion) { this.weeklyChampion = weeklyChampion; }

        public String getDailyChampion() { return dailyChampion; }
        public void setDailyChampion(String dailyChampion) { this.dailyChampion = dailyChampion; }

        public String getTrendingChampion() { return trendingChampion; }
        public void setTrendingChampion(String trendingChampion) { this.trendingChampion = trendingChampion; }

        public int getTotalChartsongs() { return totalChartsongs; }
        public void setTotalChartsongs(int totalChartsongs) { this.totalChartsongs = totalChartsongs; }

        public int getActiveArtists() { return activeArtists; }
        public void setActiveArtists(int activeArtists) { this.activeArtists = activeArtists; }
    }
}