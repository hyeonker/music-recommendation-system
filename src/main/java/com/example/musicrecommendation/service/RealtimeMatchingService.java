package com.example.musicrecommendation.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Map;

/**
 * 실시간 매칭 알림 서비스
 */
@Service
@RequiredArgsConstructor
public class RealtimeMatchingService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserProfileService userProfileService;
    private final MusicMatchingService musicMatchingService;

    /**
     * 매칭 성공 알림 전송
     */
    public void sendMatchingSuccessNotification(Long userId, Object matchingResult) {
        Object notification = new Object() {
            public final String type = "MATCHING_SUCCESS";
            public final String title = "🎵 새로운 매칭 발견!";
            public final String message = "음악 취향이 비슷한 사용자를 찾았습니다!";
            public final Object data = matchingResult;
            public final String timestamp = LocalDateTime.now().toString();
            public final boolean success = true;
        };

        // 특정 사용자에게 개인 알림 전송
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
        );

        // 전체 사용자에게 브로드캐스트 (선택적)
        messagingTemplate.convertAndSend("/topic/matching-updates", new Object() {
            public final String type = "NEW_MATCH_AVAILABLE";
            public final String message = "새로운 매칭이 생성되었습니다";
            public final String timestamp = LocalDateTime.now().toString();
        });
    }

    /**
     * 추천곡 업데이트 알림
     */
    public void sendRecommendationUpdateNotification(Long userId, Object recommendations) {
        Object notification = new Object() {
            public final String type = "RECOMMENDATION_UPDATE";
            public final String title = "🎶 새로운 추천곡 도착!";
            public final String message = "당신을 위한 새로운 음악을 발견했습니다!";
            public final Object data = recommendations;
            public final String timestamp = LocalDateTime.now().toString();
            public final boolean success = true;
        };

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/recommendations",
                notification
        );
    }

    /**
     * 실시간 매칭 상태 브로드캐스트
     */
    public void broadcastMatchingStatus() {
        Object status = new Object() {
            public final String type = "SYSTEM_STATUS";
            public final String message = "매칭 시스템 가동 중";
            public final int activeUsers = getActiveUsersCount();
            public final int totalMatches = getTotalMatchesCount();
            public final String timestamp = LocalDateTime.now().toString();
            public final boolean online = true;
        };

        messagingTemplate.convertAndSend("/topic/system-status", status);
    }

    /**
     * 실제 사용자 프로필 기반 비동기 매칭 프로세스
     */
    public CompletableFuture<Object> processAsyncMatching(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 매칭 처리 시간 (1-3초)
                Thread.sleep(1000 + (long)(Math.random() * 2000));

                // 실제 매칭 후보 찾기
                var matchCandidates = musicMatchingService.findMatchCandidates(userId, 3);
                
                if (matchCandidates.isEmpty()) {
                    // 매칭 후보가 없을 경우 시뮬레이션 데이터 반환
                    Object noMatchResult = new Object() {
                        public final boolean success = false;
                        public final String message = "현재 매칭할 수 있는 사용자가 없습니다. 나중에 다시 시도해보세요!";
                        public final String suggestion = "더 많은 음악을 좋아요 하면 매칭 확률이 높아집니다.";
                    };
                    return noMatchResult;
                }

                // 가장 유사도가 높은 사용자 선택
                var bestMatch = matchCandidates.get(0);
                var matchedUserProfile = userProfileService.getOrInit(bestMatch.getUser2Id());
                
                // 공통 장르 추출
                List<String> commonGenres = extractCommonGenres(userId, bestMatch.getUser2Id());

                // 매칭 결과 생성
                Object matchResult = new Object() {
                    public final Long matchedUserId = bestMatch.getUser2Id();
                    public final String matchedUserName = "음악친구#" + bestMatch.getUser2Id();
                    public final double compatibilityScore = bestMatch.getSimilarityScore();
                    public final String[] commonGenreArray = commonGenres.toArray(new String[0]);
                    public final String matchReason = bestMatch.getMatchReason();
                    public final int commonSongs = bestMatch.getCommonLikedSongs();
                    public final boolean success = true;
                };

                // 매칭 생성 및 저장
                musicMatchingService.createMatch(userId, bestMatch.getUser2Id());

                // 매칭 성공 알림 전송
                sendMatchingSuccessNotification(userId, matchResult);

                return matchResult;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new Object() {
                    public final boolean success = false;
                    public final String error = "매칭 처리 중 오류 발생";
                };
            } catch (Exception e) {
                return new Object() {
                    public final boolean success = false;
                    public final String error = "매칭 서비스 오류: " + e.getMessage();
                };
            }
        });
    }

    /**
     * 사용자 간 공통 장르 추출
     */
    private List<String> extractCommonGenres(Long userId1, Long userId2) {
        try {
            var profile1 = userProfileService.getOrInit(userId1);
            var profile2 = userProfileService.getOrInit(userId2);
            
            // 두 사용자의 선호 장르에서 공통점 찾기
            List<String> commonGenres = profile1.getFavoriteGenres().stream()
                .filter(genre1 -> profile2.getFavoriteGenres().stream()
                    .anyMatch(genre2 -> {
                        if (genre1 instanceof Map && genre2 instanceof Map) {
                            Map<String, Object> g1 = (Map<String, Object>) genre1;
                            Map<String, Object> g2 = (Map<String, Object>) genre2;
                            return g1.get("name").equals(g2.get("name"));
                        }
                        return false;
                    }))
                .map(genre -> {
                    if (genre instanceof Map) {
                        return (String) ((Map<String, Object>) genre).get("name");
                    }
                    return genre.toString();
                })
                .limit(3)
                .toList();
                
            return commonGenres.isEmpty() ? List.of("Pop", "Rock") : commonGenres;
        } catch (Exception e) {
            // 오류 시 기본 장르 반환
            return List.of("Pop", "Rock", "K-Pop");
        }
    }

    /**
     * 사용자별 실시간 알림 전송
     */
    public void sendPersonalNotification(Long userId, String title, String message, Object data) {
        Object notification = new Object() {
            public final String type = "PERSONAL_NOTIFICATION";
            public final String notificationTitle = title;
            public final String notificationMessage = message;
            public final Object notificationData = data;
            public final String timestamp = LocalDateTime.now().toString();
            public final boolean personal = true;
        };

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/personal",
                notification
        );
    }

    /**
     * 전체 공지사항 브로드캐스트
     */
    public void broadcastAnnouncement(String title, String message) {
        Object announcement = new Object() {
            public final String type = "ANNOUNCEMENT";
            public final String announcementTitle = title;
            public final String announcementMessage = message;
            public final String timestamp = LocalDateTime.now().toString();
            public final boolean broadcast = true;
        };

        messagingTemplate.convertAndSend("/topic/announcements", announcement);
    }

    // 헬퍼 메서드들
    private int getActiveUsersCount() {
        return 25 + (int)(Math.random() * 50); // 시뮬레이션
    }

    private int getTotalMatchesCount() {
        return 150 + (int)(Math.random() * 100); // 시뮬레이션
    }
}