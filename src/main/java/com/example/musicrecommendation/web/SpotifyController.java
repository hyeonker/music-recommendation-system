// backend/src/main/java/com/example/musicrecommendation/web/SpotifyController.java
package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.SpotifyService;
import com.example.musicrecommendation.web.dto.spotify.ArtistDto;
import com.example.musicrecommendation.web.dto.spotify.TrackDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spotify")
public class SpotifyController {

    private final SpotifyService spotifyService;

    public SpotifyController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }

    /**
     * 아티스트 검색
     * 예) GET /api/spotify/search/artists?q=Radiohead&limit=3
     */
    @GetMapping("/search/artists")
    public List<ArtistDto> searchArtists(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "limit", defaultValue = "3") int limit
    ) {
        // 1) 입력 정규화
        String query = (q == null) ? "" : q.trim();

        // 2) 빈 검색어면 호출하지 않고 바로 빈 리스트 반환
        if (query.isEmpty()) {
            return List.of();
        }

        // 3) limit 안전 범위 강제 (1~10)
        int safeLimit = Math.max(1, Math.min(limit, 10));

        // 4) 서비스 호출
        return spotifyService.searchArtists(query, safeLimit);
    }

    /**
     * 트랙 검색
     * 예) GET /api/spotify/search/tracks?q=Dynamite&limit=3
     */
    @GetMapping("/search/tracks")
    public List<TrackDto> searchTracks(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "limit", defaultValue = "3") int limit
    ) {
        String query = (q == null) ? "" : q.trim();

        if (query.isEmpty()) {
            return List.of();
        }

        int safeLimit = Math.max(1, Math.min(limit, 10));

        return spotifyService.searchTracks(query, safeLimit);
    }

    /**
     * 간단 헬스체크 (토큰 갱신 가능 여부 확인용)
     * 예) GET /api/spotify/health
     */
    @GetMapping("/health")
    public String health() {
        return spotifyService.getHealthStatus();
    }
}
