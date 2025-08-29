package com.example.musicrecommendation.service;

import com.example.musicrecommendation.web.dto.PlaylistShareRequest;
import com.example.musicrecommendation.web.dto.PlaylistShareResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PlaylistSharingService {

    private final SpotifyService spotifyService;
    private final UserProfileService userProfileService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MusicMatchingService musicMatchingService;
    private final NotificationService notificationService;

    /**
     * 매칭된 사용자에게 플레이리스트 공유
     */
    public CompletableFuture<PlaylistShareResponse> sharePlaylistToMatch(PlaylistShareRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 매칭 관계 확인
                var userMatches = musicMatchingService.getUserMatches(request.fromUserId());
                boolean isMatched = userMatches.stream()
                    .anyMatch(match -> match.getUser2Id().equals(request.toUserId()) || 
                             match.getUser1Id().equals(request.toUserId()));

                if (!isMatched) {
                    return new PlaylistShareResponse(
                        false,
                        "매칭되지 않은 사용자에게는 플레이리스트를 공유할 수 없습니다.",
                        null,
                        Instant.now()
                    );
                }

                // 플레이리스트 정보 가져오기 (시뮬레이션)
                var playlistInfo = createPlaylistInfo(request);

                // 수신자에게 실시간 알림 전송
                sendPlaylistShareNotification(request.toUserId(), request.fromUserId(), playlistInfo);
                
                // 통합 알림 서비스로 알림 전송
                try {
                    String senderName = "사용자#" + request.fromUserId();
                    String playlistName = request.playlistName() != null ? request.playlistName() : "공유된 플레이리스트";
                    notificationService.sendPlaylistShareNotification(request.toUserId(), request.fromUserId(), senderName, playlistName);
                } catch (Exception e) {
                    // 알림 실패해도 공유는 성공으로 처리
                }

                // 공유 성공 응답
                return new PlaylistShareResponse(
                    true,
                    "플레이리스트가 성공적으로 공유되었습니다!",
                    playlistInfo,
                    Instant.now()
                );

            } catch (Exception e) {
                return new PlaylistShareResponse(
                    false,
                    "플레이리스트 공유 중 오류 발생: " + e.getMessage(),
                    null,
                    Instant.now()
                );
            }
        });
    }

    /**
     * 추천 플레이리스트 생성 (사용자 프로필 기반)
     */
    public CompletableFuture<Map<String, Object>> generateRecommendedPlaylist(Long userId, Long targetUserId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var userProfile = userProfileService.getOrInit(userId);
                var targetProfile = userProfileService.getOrInit(targetUserId);

                // 공통 선호 장르 추출
                List<String> commonGenres = extractCommonGenres(userProfile, targetProfile);

                // 추천 플레이리스트 생성 (시뮬레이션)
                return Map.of(
                    "playlistId", "recommended_" + userId + "_" + targetUserId,
                    "name", "우리의 공통 취향 플레이리스트",
                    "description", "공통으로 좋아하는 장르: " + String.join(", ", commonGenres),
                    "tracks", generateTrackList(commonGenres),
                    "commonGenres", commonGenres,
                    "createdAt", Instant.now().toString(),
                    "createdBy", userId,
                    "sharedWith", targetUserId
                );

            } catch (Exception e) {
                return Map.of(
                    "error", true,
                    "message", "추천 플레이리스트 생성 실패: " + e.getMessage(),
                    "timestamp", Instant.now().toString()
                );
            }
        });
    }

    /**
     * 협업 플레이리스트 생성
     */
    public CompletableFuture<Map<String, Object>> createCollaborativePlaylist(Long user1Id, Long user2Id, String playlistName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String collaborativeId = "collab_" + Math.min(user1Id, user2Id) + "_" + Math.max(user1Id, user2Id);
                
                var playlist = Map.of(
                    "playlistId", collaborativeId,
                    "name", playlistName != null ? playlistName : "협업 플레이리스트",
                    "type", "COLLABORATIVE",
                    "collaborators", List.of(user1Id, user2Id),
                    "tracks", List.of(), // 빈 플레이리스트로 시작
                    "createdAt", Instant.now().toString(),
                    "isPublic", false,
                    "allowEditing", true
                );

                // 두 사용자에게 알림 전송
                sendCollaborativePlaylistNotification(user1Id, user2Id, playlist);

                return playlist;

            } catch (Exception e) {
                return Map.of(
                    "error", true,
                    "message", "협업 플레이리스트 생성 실패: " + e.getMessage(),
                    "timestamp", Instant.now().toString()
                );
            }
        });
    }

    /**
     * 플레이리스트에 곡 추가
     */
    public CompletableFuture<Boolean> addTrackToSharedPlaylist(String playlistId, Long userId, Map<String, Object> track) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 플레이리스트 권한 확인 (시뮬레이션)
                if (!hasPlaylistPermission(playlistId, userId)) {
                    return false;
                }

                // 곡 추가 로직 (시뮬레이션)
                log.info("사용자 {}가 플레이리스트 {}에 곡을 추가했습니다: {}", 
                    userId, playlistId, track.get("name"));

                // 협업자들에게 실시간 알림
                notifyPlaylistUpdate(playlistId, userId, "TRACK_ADDED", track);

                return true;

            } catch (Exception e) {
                log.error("곡 추가 실패: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * 공유된 플레이리스트 목록 조회
     */
    public List<Map<String, Object>> getSharedPlaylists(Long userId) {
        try {
            // 시뮬레이션 데이터 반환
            return List.of(
                Map.of(
                    "playlistId", "shared_123",
                    "name", "록 음악 모음",
                    "sharedBy", "음악친구#2",
                    "trackCount", 15,
                    "sharedAt", Instant.now().minusSeconds(3600).toString(),
                    "type", "SHARED"
                ),
                Map.of(
                    "playlistId", "collab_456",
                    "name", "우리의 협업 플레이리스트",
                    "collaborators", List.of("나", "음악친구#3"),
                    "trackCount", 8,
                    "createdAt", Instant.now().minusSeconds(7200).toString(),
                    "type", "COLLABORATIVE"
                )
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    // Helper methods
    
    private Map<String, Object> createPlaylistInfo(PlaylistShareRequest request) {
        return Map.of(
            "playlistId", request.playlistId(),
            "name", request.playlistName() != null ? request.playlistName() : "공유된 플레이리스트",
            "description", request.description() != null ? request.description() : "",
            "sharedBy", "사용자#" + request.fromUserId(),
            "sharedTo", "사용자#" + request.toUserId(),
            "trackCount", 12, // 시뮬레이션
            "sharedAt", Instant.now().toString(),
            "type", "SHARED"
        );
    }

    private void sendPlaylistShareNotification(Long toUserId, Long fromUserId, Map<String, Object> playlistInfo) {
        var notification = Map.of(
            "type", "PLAYLIST_SHARED",
            "title", "🎵 새로운 플레이리스트 공유!",
            "message", "사용자#" + fromUserId + "님이 플레이리스트를 공유했습니다",
            "playlist", playlistInfo,
            "timestamp", Instant.now().toString()
        );

        messagingTemplate.convertAndSendToUser(
            toUserId.toString(),
            "/queue/playlist-notifications",
            notification
        );
    }

    private void sendCollaborativePlaylistNotification(Long user1Id, Long user2Id, Map<String, Object> playlist) {
        var notification = Map.of(
            "type", "COLLABORATIVE_PLAYLIST_CREATED",
            "title", "🎶 협업 플레이리스트 생성!",
            "message", "새로운 협업 플레이리스트가 생성되었습니다",
            "playlist", playlist,
            "timestamp", Instant.now().toString()
        );

        messagingTemplate.convertAndSendToUser(user1Id.toString(), "/queue/playlist-notifications", notification);
        messagingTemplate.convertAndSendToUser(user2Id.toString(), "/queue/playlist-notifications", notification);
    }

    private void notifyPlaylistUpdate(String playlistId, Long userId, String action, Map<String, Object> track) {
        var notification = Map.of(
            "type", "PLAYLIST_UPDATED",
            "action", action,
            "playlistId", playlistId,
            "updatedBy", userId,
            "track", track,
            "timestamp", Instant.now().toString()
        );

        messagingTemplate.convertAndSend("/topic/playlist." + playlistId, notification);
    }

    private List<String> extractCommonGenres(Object profile1, Object profile2) {
        // 시뮬레이션
        return List.of("Pop", "Rock", "K-Pop");
    }

    private List<Map<String, Object>> generateTrackList(List<String> genres) {
        // 장르 기반 추천 곡 시뮬레이션
        return List.of(
            Map.of(
                "id", "track1",
                "name", "Recommended Song 1",
                "artist", "Popular Artist",
                "album", "Great Album",
                "genre", genres.get(0)
            ),
            Map.of(
                "id", "track2", 
                "name", "Recommended Song 2",
                "artist", "Another Artist",
                "album", "Another Album",
                "genre", genres.size() > 1 ? genres.get(1) : genres.get(0)
            )
        );
    }

    private boolean hasPlaylistPermission(String playlistId, Long userId) {
        // 권한 확인 로직 (시뮬레이션)
        return true;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PlaylistSharingService.class);
}