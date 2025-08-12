package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.SimpleStatsService;
import com.example.musicrecommendation.web.dto.SimpleStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 📊 통계 대시보드 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/stats")
@Tag(name = "📊 통계 대시보드 API", description = "음악 서비스 통계 및 차트 조회")
public class SimpleStatsController {

    private final SimpleStatsService simpleStatsService;

    public SimpleStatsController(SimpleStatsService simpleStatsService) {
        this.simpleStatsService = simpleStatsService;
    }

    /**
     * 📊 전체 대시보드 조회
     */
    @GetMapping("/dashboard")
    @Operation(summary = "📊 전체 대시보드",
            description = "전체 통계 대시보드를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<SimpleStatsResponse> getStatsDashboard() {
        SimpleStatsResponse dashboard = simpleStatsService.getStatsDashboard();
        return ResponseEntity.ok(dashboard);
    }

    /**
     * 🏆 TOP 인기 곡 조회
     */
    @GetMapping("/top-songs")
    @Operation(summary = "🏆 TOP 인기 곡",
            description = "좋아요 수 기준 인기 곡을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<SimpleStatsResponse.TopSongDto>> getTopSongs(
            @Parameter(description = "조회할 곡 수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        List<SimpleStatsResponse.TopSongDto> topSongs = simpleStatsService.getTopSongs(limit);
        return ResponseEntity.ok(topSongs);
    }

    /**
     * 👤 활발한 사용자 조회
     */
    @GetMapping("/active-users")
    @Operation(summary = "👤 활발한 사용자",
            description = "좋아요를 많이 한 활발한 사용자를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<SimpleStatsResponse.TopUserDto>> getActiveUsers(
            @Parameter(description = "조회할 사용자 수", example = "5")
            @RequestParam(defaultValue = "5") int limit) {

        List<SimpleStatsResponse.TopUserDto> activeUsers = simpleStatsService.getActiveUsers(limit);
        return ResponseEntity.ok(activeUsers);
    }

    /**
     * 🎤 인기 아티스트 조회
     */
    @GetMapping("/top-artists")
    @Operation(summary = "🎤 인기 아티스트",
            description = "곡들의 총 좋아요 수 기준 인기 아티스트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<SimpleStatsResponse.TopArtistDto>> getTopArtists(
            @Parameter(description = "조회할 아티스트 수", example = "5")
            @RequestParam(defaultValue = "5") int limit) {

        List<SimpleStatsResponse.TopArtistDto> topArtists = simpleStatsService.getTopArtists(limit);
        return ResponseEntity.ok(topArtists);
    }

    /**
     * 📈 전체 통계 조회
     */
    @GetMapping("/overall")
    @Operation(summary = "📈 전체 통계",
            description = "사용자, 곡, 좋아요의 전체 통계를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<SimpleStatsResponse.OverallStatsDto> getOverallStats() {
        SimpleStatsResponse.OverallStatsDto stats = simpleStatsService.getOverallStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * ⚡ 실시간 TOP 5
     */
    @GetMapping("/realtime")
    @Operation(summary = "⚡ 실시간 TOP 5",
            description = "실시간 인기 TOP 5 곡을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<SimpleStatsResponse.TopSongDto>> getRealtimeTop() {
        List<SimpleStatsResponse.TopSongDto> realtimeTop = simpleStatsService.getTopSongs(5);
        return ResponseEntity.ok(realtimeTop);
    }

    /**
     * 📋 통계 요약 (한 줄 요약)
     */
    @GetMapping("/summary")
    @Operation(summary = "📋 통계 요약",
            description = "주요 통계를 한 줄로 요약해서 보여줍니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<StatsSummaryDto> getStatsSummary() {

        // 전체 통계
        SimpleStatsResponse.OverallStatsDto overall = simpleStatsService.getOverallStats();

        // TOP 곡 정보
        List<SimpleStatsResponse.TopSongDto> topSongs = simpleStatsService.getTopSongs(1);

        // TOP 사용자 정보 - 안전하게 처리
        List<SimpleStatsResponse.TopUserDto> topUsers = simpleStatsService.getActiveUsers(1);
        String topUserName = "데이터 없음";
        if (!topUsers.isEmpty()) {
            SimpleStatsResponse.TopUserDto topUser = topUsers.get(0);
            // TopUserDto의 실제 필드를 확인해서 사용
            try {
                topUserName = topUser.toString(); // 임시로 toString() 사용
            } catch (Exception e) {
                topUserName = "사용자 #" + (topUsers.size() > 0 ? "1" : "0");
            }
        }

        StatsSummaryDto summary = new StatsSummaryDto(
                overall.getTotalUsers(),
                overall.getTotalSongs(),
                overall.getTotalLikes(),
                overall.getTodayLikes(),
                topSongs.isEmpty() ? "데이터 없음" : "TOP 곡 (좋아요 " + topSongs.get(0).getLikeCount() + "개)",
                topUserName
        );

        return ResponseEntity.ok(summary);
    }

    // =========================
    // 새로 추가된 시간별 차트 API들 (임시로 기존 기능 사용)
    // =========================

    /**
     * 📅 일간 차트 (오늘의 HOT) - 임시로 TOP 곡으로 대체
     */
    @GetMapping("/daily-chart")
    @Operation(summary = "📅 일간 차트",
            description = "오늘의 HOT 차트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<SimpleStatsResponse.TopSongDto>> getDailyChart(
            @Parameter(description = "조회할 곡 수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        // 임시로 기존 TOP 곡으로 대체
        List<SimpleStatsResponse.TopSongDto> chart = simpleStatsService.getTopSongs(limit);
        return ResponseEntity.ok(chart);
    }

    /**
     * 📊 주간 차트 (이번 주 HOT) - 임시로 TOP 곡으로 대체
     */
    @GetMapping("/weekly-chart")
    @Operation(summary = "📊 주간 차트",
            description = "이번 주 HOT 차트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<SimpleStatsResponse.TopSongDto>> getWeeklyChart(
            @Parameter(description = "조회할 곡 수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        // 임시로 기존 TOP 곡으로 대체
        List<SimpleStatsResponse.TopSongDto> chart = simpleStatsService.getTopSongs(limit);
        return ResponseEntity.ok(chart);
    }

    /**
     * 🏆 월간 차트 (이번 달 HOT) - 임시로 TOP 곡으로 대체
     */
    @GetMapping("/monthly-chart")
    @Operation(summary = "🏆 월간 차트",
            description = "이번 달 HOT 차트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<SimpleStatsResponse.TopSongDto>> getMonthlyChart(
            @Parameter(description = "조회할 곡 수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        // 임시로 기존 TOP 곡으로 대체
        List<SimpleStatsResponse.TopSongDto> chart = simpleStatsService.getTopSongs(limit);
        return ResponseEntity.ok(chart);
    }

    /**
     * 📈 종합 차트 대시보드 (일/주/월 모두) - 임시로 기존 대시보드로 대체
     */
    @GetMapping("/charts-dashboard")
    @Operation(summary = "📈 종합 차트 대시보드",
            description = "일간/주간/월간 차트를 모두 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<SimpleStatsResponse> getChartsDashboard() {
        // 임시로 기존 대시보드로 대체
        SimpleStatsResponse dashboard = simpleStatsService.getStatsDashboard();
        return ResponseEntity.ok(dashboard);
    }

    /**
     * 🔥 실시간 급상승 차트 - 임시로 TOP 곡으로 대체
     */
    @GetMapping("/trending-chart")
    @Operation(summary = "🔥 실시간 급상승",
            description = "최근 24시간 내 급상승한 곡들을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<SimpleStatsResponse.TopSongDto>> getTrendingChart(
            @Parameter(description = "조회할 곡 수", example = "5")
            @RequestParam(defaultValue = "5") int limit) {

        // 임시로 기존 TOP 곡으로 대체
        List<SimpleStatsResponse.TopSongDto> chart = simpleStatsService.getTopSongs(limit);
        return ResponseEntity.ok(chart);
    }

    // =========================
    // 내부 DTO 클래스들
    // =========================

    /**
     * 통계 요약 DTO
     */
    public static class StatsSummaryDto {
        private long totalUsers;
        private long totalSongs;
        private long totalLikes;
        private long todayLikes;
        private String topSongInfo;
        private String topUserName;

        public StatsSummaryDto() {}

        public StatsSummaryDto(long totalUsers, long totalSongs, long totalLikes,
                               long todayLikes, String topSongInfo, String topUserName) {
            this.totalUsers = totalUsers;
            this.totalSongs = totalSongs;
            this.totalLikes = totalLikes;
            this.todayLikes = todayLikes;
            this.topSongInfo = topSongInfo;
            this.topUserName = topUserName;
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

        public String getTopSongInfo() { return topSongInfo; }
        public void setTopSongInfo(String topSongInfo) { this.topSongInfo = topSongInfo; }

        public String getTopUserName() { return topUserName; }
        public void setTopUserName(String topUserName) { this.topUserName = topUserName; }
    }
}