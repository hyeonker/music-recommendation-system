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
            // 시뮬레이션 데이터 반환
            Map<String, Object> playlistDetails = Map.of(
                "playlistId", playlistId,
                "name", "샘플 플레이리스트",
                "description", "설명",
                "trackCount", 10,
                "duration", "45:23",
                "isPublic", false,
                "type", playlistId.startsWith("collab_") ? "COLLABORATIVE" : "SHARED",
                "createdAt", java.time.Instant.now().minusSeconds(3600).toString(),
                "tracks", List.of(
                    Map.of(
                        "id", "1",
                        "name", "Sample Song 1",
                        "artist", "Sample Artist",
                        "duration", "3:45"
                    ),
                    Map.of(
                        "id", "2",
                        "name", "Sample Song 2", 
                        "artist", "Another Artist",
                        "duration", "4:12"
                    )
                )
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
            // 시뮬레이션 검색 결과
            List<Map<String, Object>> searchResults = List.of(
                Map.of(
                    "playlistId", "search_result_1",
                    "name", query + " 관련 플레이리스트 1",
                    "description", "검색된 플레이리스트",
                    "trackCount", 15,
                    "isPublic", true
                ),
                Map.of(
                    "playlistId", "search_result_2", 
                    "name", query + " 관련 플레이리스트 2",
                    "description", "또 다른 검색 결과",
                    "trackCount", 8,
                    "isPublic", true
                )
            );

            return ResponseEntity.ok(searchResults.stream().limit(limit).toList());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(List.of(
                Map.of("error", true, "message", e.getMessage())
            ));
        }
    }
}