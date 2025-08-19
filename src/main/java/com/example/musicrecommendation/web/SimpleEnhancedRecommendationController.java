package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.SpotifyTrackService;
import com.example.musicrecommendation.web.dto.spotify.SpotifyDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/enhanced-recommendations")
@CrossOrigin(origins = "${app.cors.allowed-origin:http://localhost:3000}")
@Tag(name = "Enhanced Recommendations", description = "Spotify 기반 고도화된 추천 시스템")
public class SimpleEnhancedRecommendationController {

    // ⬇️ 변경: 삭제한 SpotifyApiService 대신 대체 서비스 주입
    private final SpotifyTrackService spotifyTrackService;

    public SimpleEnhancedRecommendationController(SpotifyTrackService spotifyTrackService) {
        this.spotifyTrackService = spotifyTrackService;
    }

    @GetMapping("/status")
    @Operation(summary = "고도화 추천 시스템 상태", description = "Spotify 연동 추천 시스템 상태 확인")
    public ResponseEntity<?> getStatus() {
        boolean connected = spotifyTrackService.testConnection();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "spotifyEnabled", connected,
                "time", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/trending")
    @Operation(summary = "장르별 인기 곡 추천", description = "Spotify 기반 간단 인기 곡 추천")
    public ResponseEntity<?> getTrending(
            @Parameter(description = "장르 키워드 (예: pop, rock, k-pop)")
            @RequestParam(defaultValue = "pop") String genre,
            @Parameter(description = "최대 반환 개수")
            @RequestParam(defaultValue = "10") int limit
    ) {
        var opt = spotifyTrackService.getPopularTracks(genre, limit);
        if (opt.isEmpty() || opt.get().getTracks() == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        // 프론트에서 쓰기 쉽게 items만 내려줌
        return ResponseEntity.ok(opt.get().getTracks().getItems());
    }
}
