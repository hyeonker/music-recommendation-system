// backend/src/main/java/com/example/musicrecommendation/web/SpotifyController.java
package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.SpotifyService;
import com.example.musicrecommendation.web.dto.spotify.ArtistDto;
import com.example.musicrecommendation.web.dto.spotify.TrackDto;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 특정 아티스트의 곡 검색
     * 예) GET /api/spotify/search/artist-tracks?artist=Radiohead&q=there&limit=10
     */
    @GetMapping("/search/artist-tracks")
    public List<TrackDto> searchArtistTracks(
            @RequestParam(name = "artist", required = true) String artist,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        String query = (q == null) ? "" : q.trim();
        String artistName = artist.trim();

        if (query.isEmpty() || artistName.isEmpty()) {
            return List.of();
        }

        int safeLimit = Math.max(1, Math.min(limit, 20));

        return spotifyService.searchArtistTracks(artistName, query, safeLimit);
    }

    /**
     * 아티스트의 인기 곡 가져오기
     * 예) GET /api/spotify/artist/4Z8W4fKeB5YxbusRsdQVPb/top-tracks?market=KR&limit=10
     */
    @GetMapping("/artist/{artistId}/top-tracks")
    public List<TrackDto> getArtistTopTracks(
            @PathVariable String artistId,
            @RequestParam(name = "market", defaultValue = "KR") String market,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return spotifyService.getArtistTopTracks(artistId, market, safeLimit);
    }


    /**
     * 통합 검색 (MusicDiscovery 페이지용)
     * 예) GET /api/spotify/search?q=Radiohead&type=track&limit=20
     */
    @GetMapping("/search")
    public Map<String, Object> unifiedSearch(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "type", defaultValue = "track") String type,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        String query = (q == null) ? "" : q.trim();
        
        if (query.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            if ("track".equals(type)) {
                emptyResult.put("tracks", Map.of("items", List.of()));
            } else if ("artist".equals(type)) {
                emptyResult.put("artists", Map.of("items", List.of()));
            }
            return emptyResult;
        }

        int safeLimit = Math.max(1, Math.min(limit, 50));
        
        Map<String, Object> result = new HashMap<>();
        
        if ("track".equals(type)) {
            List<TrackDto> tracks = spotifyService.searchTracks(query, safeLimit);
            List<Map<String, Object>> spotifyFormatTracks = tracks.stream()
                .map(this::convertToSpotifyFormat)
                .toList();
            result.put("tracks", Map.of("items", spotifyFormatTracks));
        } else if ("artist".equals(type)) {
            List<ArtistDto> artists = spotifyService.searchArtists(query, safeLimit);
            result.put("artists", Map.of("items", artists));
        } else {
            List<TrackDto> tracks = spotifyService.searchTracks(query, safeLimit);
            List<Map<String, Object>> spotifyFormatTracks = tracks.stream()
                .map(this::convertToSpotifyFormat)
                .toList();
            result.put("tracks", Map.of("items", spotifyFormatTracks));
        }
        
        return result;
    }

    private Map<String, Object> convertToSpotifyFormat(TrackDto track) {
        Map<String, Object> spotifyTrack = new HashMap<>();
        spotifyTrack.put("id", track.getId());
        spotifyTrack.put("name", track.getName());
        spotifyTrack.put("popularity", track.getPopularity());
        spotifyTrack.put("duration_ms", track.getDurationMs());
        spotifyTrack.put("preview_url", track.getPreviewUrl());
        
        // artists 배열
        List<Map<String, Object>> artists = track.getArtists().stream()
            .map(artist -> {
                Map<String, Object> artistMap = new HashMap<>();
                artistMap.put("name", artist.getName());
                artistMap.put("id", artist.getId());
                return artistMap;
            })
            .toList();
        spotifyTrack.put("artists", artists);
        
        // album 객체
        if (track.getAlbum() != null) {
            Map<String, Object> album = new HashMap<>();
            album.put("name", track.getAlbum().getName());
            album.put("id", track.getAlbum().getId());
            
            // images 배열
            if (track.getAlbum().getImages() != null && !track.getAlbum().getImages().isEmpty()) {
                album.put("images", track.getAlbum().getImages());
            } else {
                album.put("images", List.of());
            }
            
            spotifyTrack.put("album", album);
        }
        
        // external_urls 객체
        Map<String, String> externalUrls = new HashMap<>();
        String spotifyUrl = track.getSpotifyUrl();
        if (spotifyUrl == null || spotifyUrl.isEmpty()) {
            spotifyUrl = "https://open.spotify.com/track/" + track.getId();
        }
        externalUrls.put("spotify", spotifyUrl);
        spotifyTrack.put("external_urls", externalUrls);
        
        return spotifyTrack;
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
