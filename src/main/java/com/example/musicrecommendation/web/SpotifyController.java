package com.example.musicrecommendation.web;

import com.example.musicrecommendation.web.dto.spotify.ArtistDto;
import com.example.musicrecommendation.service.SpotifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spotify")
@CrossOrigin(origins = "http://localhost:3000")
public class SpotifyController {

    @Autowired
    private SpotifyService spotifyService;

    /**
     * 아티스트 검색 API
     * GET /api/spotify/search/artists?q=검색어&limit=10
     */
    @GetMapping("/search/artists")
    public ResponseEntity<List<ArtistDto>> searchArtists(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {

        try {
            System.out.println("아티스트 검색 요청: " + q + ", limit: " + limit);
            List<ArtistDto> artists = spotifyService.searchArtists(q, limit);
            System.out.println("검색 결과: " + artists.size() + "개 아티스트 발견");
            return ResponseEntity.ok(artists);
        } catch (Exception e) {
            System.err.println("아티스트 검색 API 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 특정 아티스트 정보 조회
     * GET /api/spotify/artists/{artistId}
     */
    @GetMapping("/artists/{artistId}")
    public ResponseEntity<ArtistDto> getArtist(@PathVariable String artistId) {
        // 추후 구현 예정
        System.out.println("아티스트 상세 정보 요청: " + artistId);
        return ResponseEntity.notFound().build();
    }

    /**
     * Spotify API 연결 상태 확인
     * GET /api/spotify/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        try {
            String status = spotifyService.getHealthStatus();
            System.out.println("Spotify 상태 확인: " + status);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            System.err.println("Spotify 상태 확인 실패: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("Spotify API 연결 실패: " + e.getMessage());
        }
    }

    /**
     * 테스트용 간단한 엔드포인트
     * GET /api/spotify/test
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Spotify Controller 작동 중!");
    }
}