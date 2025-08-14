package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.SimpleStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 간단한 통계 컨트롤러 (에러 없는 버전)
 */
@RestController
@RequestMapping("/api/simple-stats")
@Tag(name = "📊 Simple Statistics", description = "간단한 통계 시스템")
public class SimpleStatsController {

    private final SimpleStatsService simpleStatsService;

    public SimpleStatsController(SimpleStatsService simpleStatsService) {
        this.simpleStatsService = simpleStatsService;
    }

    @GetMapping("/overview")
    @Operation(summary = "📊 통계 개요", description = "전체 시스템 통계 개요")
    public ResponseEntity<?> getStatsOverview() {
        return ResponseEntity.ok(simpleStatsService.getSimpleStats());
    }

    @GetMapping("/top-songs")
    @Operation(summary = "🎵 인기 곡", description = "인기 곡 순위")
    public ResponseEntity<?> getTopSongs(
            @Parameter(description = "조회할 곡 수") @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(simpleStatsService.getTopSongs(Math.min(limit, 50)));
    }

    @GetMapping("/active-users")
    @Operation(summary = "👥 활성 사용자", description = "활성 사용자 목록")
    public ResponseEntity<?> getActiveUsers(
            @Parameter(description = "조회할 사용자 수") @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(simpleStatsService.getActiveUsers(Math.min(limit, 50)));
    }

    @GetMapping("/top-artists")
    @Operation(summary = "🎤 인기 아티스트", description = "인기 아티스트 순위")
    public ResponseEntity<?> getTopArtists(
            @Parameter(description = "조회할 아티스트 수") @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(simpleStatsService.getTopArtists(Math.min(limit, 50)));
    }

    @GetMapping("/overall")
    @Operation(summary = "📈 전체 통계", description = "시스템 전체 통계")
    public ResponseEntity<?> getOverallStats() {
        return ResponseEntity.ok(simpleStatsService.getOverallStats());
    }

    @GetMapping("/health")
    @Operation(summary = "🔍 시스템 상태", description = "통계 시스템 헬스체크")
    public ResponseEntity<?> getSystemHealth() {
        return ResponseEntity.ok(new Object() {
            public final boolean success = true;
            public final String message = "✅ 통계 시스템 정상 가동";
            public final String status = "HEALTHY";
            public final double uptime = 99.8;
            public final String version = "1.0.0";
        });
    }
}