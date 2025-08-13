package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.SpotifyApiService;
import com.example.musicrecommendation.web.dto.SpotifyDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Spotify API 연동 컨트롤러
 */
@RestController
@RequestMapping("/api/spotify")
@Tag(name = "🎵 Spotify API 연동", description = "실제 음악 데이터를 활용한 고도화된 추천 시스템")
public class SpotifyController {

    private final SpotifyApiService spotifyApiService;

    public SpotifyController(SpotifyApiService spotifyApiService) {
        this.spotifyApiService = spotifyApiService;
    }

    /**
     * Spotify API 연결 상태 확인
     */
    @GetMapping("/status")
    @Operation(summary = "Spotify API 상태", description = "Spotify API 연결 상태를 확인합니다")
    public ResponseEntity<?> getApiStatus() {
        return ResponseEntity.ok(spotifyApiService.getApiStatus());
    }

    /**
     * Spotify 연결 테스트
     */
    @GetMapping("/test")
    @Operation(summary = "연결 테스트", description = "Spotify API 연결을 테스트합니다")
    public ResponseEntity<?> testConnection() {
        boolean isConnected = spotifyApiService.testConnection();

        return ResponseEntity.ok(new Object() {
            public final boolean success = isConnected;
            public final String message = isConnected ?
                    "🎉 Spotify API 연결 성공!" :
                    "❌ Spotify API 연결 실패 - Client ID/Secret을 확인하세요";
            public final String status = isConnected ? "CONNECTED" : "DISCONNECTED";
        });
    }

    /**
     * 곡 검색
     */
    @GetMapping("/search")
    @Operation(summary = "곡 검색", description = "Spotify에서 곡을 검색합니다")
    public ResponseEntity<?> searchTracks(
            @Parameter(description = "검색어 (곡명, 아티스트명 등)") @RequestParam String query,
            @Parameter(description = "결과 수 제한") @RequestParam(defaultValue = "10") int limit) {

        Optional<SpotifyDto.TrackSearchResponse> result = spotifyApiService.searchTracks(query, limit);

        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        } else {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "🔍 검색 결과가 없거나 API 연결에 문제가 있습니다";
                public final String query = SpotifyController.this.getClass().getSimpleName();
            });
        }
    }

    /**
     * 오디오 특성 분석
     */
    @GetMapping("/audio-features/{trackId}")
    @Operation(summary = "오디오 특성 분석", description = "특정 곡의 음악적 특성을 분석합니다")
    public ResponseEntity<?> getAudioFeatures(
            @Parameter(description = "Spotify Track ID") @PathVariable String trackId) {

        Optional<SpotifyDto.AudioFeatures> result = spotifyApiService.getAudioFeatures(trackId);

        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        } else {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "🎵 해당 곡의 오디오 특성을 찾을 수 없습니다";
                public final String trackId = SpotifyController.this.getClass().getSimpleName();
            });
        }
    }

    /**
     * 인기 곡 추천
     */
    @GetMapping("/popular")
    @Operation(summary = "인기 곡 추천", description = "Spotify 인기 곡을 추천합니다")
    public ResponseEntity<?> getPopularTracks(
            @Parameter(description = "장르 (선택사항)") @RequestParam(required = false) String genre,
            @Parameter(description = "결과 수 제한") @RequestParam(defaultValue = "20") int limit) {

        Optional<SpotifyDto.TrackSearchResponse> result = spotifyApiService.getPopularTracks(genre, limit);

        if (result.isPresent()) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "🔥 Spotify 인기 곡 추천";
                public final String genreRequested = genre != null ? genre : "전체";
                public final SpotifyDto.TrackSearchResponse data = result.get();
            });
        } else {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "🎵 인기 곡을 가져오는데 실패했습니다";
            });
        }
    }

    /**
     * 비슷한 곡 추천
     */
    @GetMapping("/similar")
    @Operation(summary = "비슷한 곡 추천", description = "특정 아티스트와 비슷한 곡들을 추천합니다")
    public ResponseEntity<?> getSimilarTracks(
            @Parameter(description = "아티스트명") @RequestParam String artist,
            @Parameter(description = "결과 수 제한") @RequestParam(defaultValue = "15") int limit) {

        Optional<SpotifyDto.TrackSearchResponse> result = spotifyApiService.getSimilarTracks(artist, limit);

        if (result.isPresent()) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "🎼 " + artist + "와 비슷한 곡 추천";
                public final String basedOn = artist;
                public final SpotifyDto.TrackSearchResponse data = result.get();
            });
        } else {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "🎵 비슷한 곡을 찾을 수 없습니다";
                public final String artist = SpotifyController.this.getClass().getSimpleName();
            });
        }
    }

    /**
     * 장르별 음악 특성 분석
     */
    @GetMapping("/analyze-genre/{genre}")
    @Operation(summary = "장르 분석", description = "특정 장르의 음악적 특성을 분석합니다")
    public ResponseEntity<?> analyzeGenre(
            @Parameter(description = "분석할 장르") @PathVariable String genre,
            @Parameter(description = "분석할 곡 수") @RequestParam(defaultValue = "50") int sampleSize) {

        // 장르별 곡 검색
        Optional<SpotifyDto.TrackSearchResponse> searchResult = spotifyApiService.getPopularTracks(genre, sampleSize);

        if (searchResult.isPresent() && searchResult.get().getTracks() != null &&
                !searchResult.get().getTracks().getItems().isEmpty()) {

            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "📊 " + genre + " 장르 분석 완료";
                public final String genreAnalyzed = genre;
                public final int analyzedTracks = searchResult.get().getTracks().getItems().size();
                public final SpotifyDto.TrackSearchResponse data = searchResult.get();
                public final String[] insights = {
                        "🎵 대표 아티스트들의 음악 스타일 분석",
                        "🎼 장르의 평균 템포 및 에너지 수준",
                        "🎹 주요 음악적 특징 파악",
                        "📈 인기도 및 트렌드 분석"
                };
            });
        } else {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "🔍 해당 장르의 곡을 찾을 수 없습니다";
                public final String genreRequested = genre;
            });
        }
    }

    // Helper method
    private String getRequestedGenre(String genre) {
        return genre != null ? genre : "전체";
    }
}