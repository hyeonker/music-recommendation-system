package com.example.musicrecommendation.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 실시간 매칭 알림 서비스
 */
@Service
public class RealtimeMatchingService {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeMatchingService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

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
     * 비동기 매칭 프로세스 시뮬레이션
     */
    public CompletableFuture<Object> processAsyncMatching(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 매칭 처리 시뮬레이션 (2-5초)
                Thread.sleep(2000 + (long)(Math.random() * 3000));

                // 매칭 결과 생성
                Object matchResult = new Object() {
                    public final Long matchedUserId = (long)(Math.random() * 100) + 1;
                    public final String matchedUserName = "음악친구" + (int)(Math.random() * 100);
                    public final double compatibilityScore = 0.7 + (Math.random() * 0.3);
                    public final String[] commonGenres = {"K-POP", "Pop", "R&B"};
                    public final String matchReason = "비슷한 음악 취향을 가지고 있습니다";
                };

                // 매칭 성공 알림 전송
                sendMatchingSuccessNotification(userId, matchResult);

                return matchResult;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new Object() {
                    public final boolean success = false;
                    public final String error = "매칭 처리 중 오류 발생";
                };
            }
        });
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