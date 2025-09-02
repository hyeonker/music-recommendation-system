package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.PlaylistSharingService;
import com.example.musicrecommendation.web.dto.PlaylistShareRequest;
import com.example.musicrecommendation.web.dto.PlaylistShareResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PlaylistSharingController {

    private final PlaylistSharingService playlistSharingService;

    /**
     * 매칭된 사용자에게 플레이리스트 공유
     */
    @PostMapping("/share")
    public CompletableFuture<ResponseEntity<PlaylistShareResponse>> sharePlaylist(
            @RequestBody PlaylistShareRequest request) {
        return playlistSharingService.sharePlaylistToMatch(request)
            .thenApply(ResponseEntity::ok)
            .exceptionally(ex -> ResponseEntity.internalServerError()
                .body(new PlaylistShareResponse(false, 
                    "서버 오류: " + ex.getMessage(), null, java.time.Instant.now())));
    }

    /**
     * 추천 플레이리스트 생성
     */
    @PostMapping("/recommended/{targetUserId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> generateRecommendedPlaylist(
            @PathVariable Long targetUserId,
            @RequestParam Long userId) {
        return playlistSharingService.generateRecommendedPlaylist(userId, targetUserId)
            .thenApply(ResponseEntity::ok)
            .exceptionally(ex -> {
                Map<String, Object> errorResponse = new java.util.HashMap<>();
                errorResponse.put("error", true);
                errorResponse.put("message", ex.getMessage());
                return ResponseEntity.internalServerError().body(errorResponse);
            });
    }

    /**
     * 협업 플레이리스트 생성
     */
    @PostMapping("/collaborative")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createCollaborativePlaylist(
            @RequestParam Long user1Id,
            @RequestParam Long user2Id,
            @RequestParam(required = false) String playlistName) {
        return playlistSharingService.createCollaborativePlaylist(user1Id, user2Id, playlistName)
            .thenApply(ResponseEntity::ok)
            .exceptionally(ex -> {
                Map<String, Object> errorResponse = new java.util.HashMap<>();
                errorResponse.put("error", true);
                errorResponse.put("message", ex.getMessage());
                return ResponseEntity.internalServerError().body(errorResponse);
            });
    }

    /**
     * 플레이리스트에 곡 추가
     */
    @PostMapping("/{playlistId}/tracks")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> addTrack(
            @PathVariable String playlistId,
            @RequestParam Long userId,
            @RequestBody Map<String, Object> track) {
        return playlistSharingService.addTrackToSharedPlaylist(playlistId, userId, track)
            .thenApply(success -> {
                Map<String, Object> response = new java.util.HashMap<>();
                if (success) {
                    response.put("success", true);
                    response.put("message", "곡이 성공적으로 추가되었습니다");
                    response.put("timestamp", java.time.Instant.now().toString());
                    return ResponseEntity.ok(response);
                } else {
                    response.put("success", false);
                    response.put("message", "곡 추가에 실패했습니다");
                    response.put("timestamp", java.time.Instant.now().toString());
                    return ResponseEntity.badRequest().body(response);
                }
            })
            .exceptionally(ex -> {
                Map<String, Object> errorResponse = new java.util.HashMap<>();
                errorResponse.put("error", true);
                errorResponse.put("message", ex.getMessage());
                return ResponseEntity.internalServerError().body(errorResponse);
            });
    }

    /**
     * 공유된 플레이리스트 목록 조회
     */
    @GetMapping("/shared/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getSharedPlaylists(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> playlists = playlistSharingService.getSharedPlaylists(userId);
            return ResponseEntity.ok(playlists);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(List.of(
                Map.of("error", true, "message", e.getMessage())
            ));
        }
    }

    /**
     * 플레이리스트 상세 정보 조회
     */
    @GetMapping("/{playlistId}")
    public ResponseEntity<Map<String, Object>> getPlaylistDetails(@PathVariable String playlistId) {
        try {
            // 플레이리스트 서비스에서 상세 정보 조회
            List<Map<String, Object>> playlists = playlistSharingService.getSharedPlaylists(1L); // 예시 ID
            
            Map<String, Object> playlistDetails = Map.of(
                "playlistId", playlistId,
                "name", "공유된 플레이리스트",
                "description", "실제 데이터 기반 플레이리스트",
                "trackCount", playlists.size(),
                "duration", "45:23",
                "isPublic", false,
                "type", playlistId.startsWith("collab_") ? "COLLABORATIVE" : "SHARED",
                "createdAt", java.time.Instant.now().minusSeconds(3600).toString(),
                "tracks", playlists
            );

            return ResponseEntity.ok(playlistDetails);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * 플레이리스트 검색
     */
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchPlaylists(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            // 기본적인 검색 결과 반환 (서비스 계층에서 실제 검색 로직 필요)
            List<Map<String, Object>> allPlaylists = playlistSharingService.getSharedPlaylists(1L);
            
            List<Map<String, Object>> searchResults = allPlaylists.stream()
                .filter(playlist -> 
                    playlist.get("name") != null && 
                    playlist.get("name").toString().toLowerCase().contains(query.toLowerCase()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(searchResults);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(List.of(
                Map.of("error", true, "message", e.getMessage())
            ));
        }
    }
}