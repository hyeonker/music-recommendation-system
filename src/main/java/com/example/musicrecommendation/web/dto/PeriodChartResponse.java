package com.example.musicrecommendation.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 시간별 차트 응답 DTO (간단 버전)
 */
@Schema(description = "시간별 차트")
public class PeriodChartResponse {

    @Schema(description = "차트 제목", example = "📅 8월 12일 일간 차트")
    private String chartTitle;

    @Schema(description = "차트 기간", example = "2025-08-12")
    private String period;

    @Schema(description = "차트 곡 목록")
    private List<SimpleStatsResponse.TopSongDto> songs;

    @Schema(description = "생성 시간")
    private LocalDateTime updatedAt;

    // 생성자
    public PeriodChartResponse() {
        this.updatedAt = LocalDateTime.now();
    }

    public PeriodChartResponse(String chartTitle, String period, List<SimpleStatsResponse.TopSongDto> songs) {
        this();
        this.chartTitle = chartTitle;
        this.period = period;
        this.songs = songs;
    }

    // Getters and Setters
    public String getChartTitle() { return chartTitle; }
    public void setChartTitle(String chartTitle) { this.chartTitle = chartTitle; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public List<SimpleStatsResponse.TopSongDto> getSongs() { return songs; }
    public void setSongs(List<SimpleStatsResponse.TopSongDto> songs) { this.songs = songs; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}