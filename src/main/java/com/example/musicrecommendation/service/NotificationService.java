package com.example.musicrecommendation.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 통합 알림 서비스
 * - 매칭 알림
 * - 메시지 알림
 * - 플레이리스트 공유 알림
 * - 추천 알림
 * - 실시간 WebSocket 알림
 * - 알림 기록 관리
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    
    private final SimpMessagingTemplate messagingTemplate;
    
    // 메모리 기반 알림 저장소 (실제 구현에서는 데이터베이스 사용)
    private final Map<Long, List<NotificationData>> userNotifications = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> userSubscriptions = new ConcurrentHashMap<>();
    
    /**
     * 새로운 매칭 알림 전송
     */
    @Async
    public CompletableFuture<Void> sendMatchNotification(Long userId, Long matchedUserId, 
                                                        double compatibilityScore, String matchReason) {
        return CompletableFuture.runAsync(() -> {
            try {
                String title = "🎵 새로운 음악 친구 발견!";
                String message = String.format("%.0f%% 음악 취향 호환성을 가진 새로운 친구가 매칭되었습니다", 
                    compatibilityScore * 100);
                
                NotificationData notification = createNotification(
                    "MATCH_FOUND",
                    title,
                    message,
                    Map.of(
                        "matchedUserId", matchedUserId,
                        "compatibilityScore", compatibilityScore,
                        "matchReason", matchReason,
                        "actionUrl", "/matching/" + matchedUserId
                    )
                );
                
                // 사용자에게 실시간 알림 전송
                sendRealTimeNotification(userId, notification);
                
                // 알림 기록에 저장
                saveNotification(userId, notification);
                
                log.info("매칭 알림 전송 완료: userId={}, matchedUserId={}, score={}", 
                    userId, matchedUserId, compatibilityScore);
                    
            } catch (Exception e) {
                log.error("매칭 알림 전송 실패: userId={}, error={}", userId, e.getMessage());
            }
        });
    }
    
    /**
     * 새로운 채팅 메시지 알림 전송
     */
    @Async
    public CompletableFuture<Void> sendMessageNotification(Long receiverId, Long senderId, 
                                                          String senderName, String messagePreview) {
        return CompletableFuture.runAsync(() -> {
            try {
                String title = "💬 새 메시지 도착";
                String message = String.format("%s님으로부터 새 메시지가 도착했습니다", senderName);
                
                NotificationData notification = createNotification(
                    "NEW_MESSAGE",
                    title,
                    message,
                    Map.of(
                        "senderId", senderId,
                        "senderName", senderName,
                        "messagePreview", messagePreview.length() > 50 ? 
                            messagePreview.substring(0, 47) + "..." : messagePreview,
                        "actionUrl", "/chat/" + senderId
                    )
                );
                
                // 수신자에게 실시간 알림 전송 (발신자가 아닌 경우에만)
                if (!receiverId.equals(senderId)) {
                    sendRealTimeNotification(receiverId, notification);
                    saveNotification(receiverId, notification);
                }
                
                log.debug("메시지 알림 전송: receiverId={}, senderId={}", receiverId, senderId);
                
            } catch (Exception e) {
                log.error("메시지 알림 전송 실패: receiverId={}, error={}", receiverId, e.getMessage());
            }
        });
    }
    
    /**
     * 플레이리스트 공유 알림 전송
     */
    @Async
    public CompletableFuture<Void> sendPlaylistShareNotification(Long receiverId, Long senderId, 
                                                                String senderName, String playlistName) {
        return CompletableFuture.runAsync(() -> {
            try {
                String title = "🎶 플레이리스트 공유";
                String message = String.format("%s님이 플레이리스트 '%s'을(를) 공유했습니다", 
                    senderName, playlistName);
                
                NotificationData notification = createNotification(
                    "PLAYLIST_SHARED",
                    title,
                    message,
                    Map.of(
                        "senderId", senderId,
                        "senderName", senderName,
                        "playlistName", playlistName,
                        "actionUrl", "/playlists/shared"
                    )
                );
                
                sendRealTimeNotification(receiverId, notification);
                saveNotification(receiverId, notification);
                
                log.info("플레이리스트 공유 알림 전송: receiverId={}, playlistName={}", 
                    receiverId, playlistName);
                    
            } catch (Exception e) {
                log.error("플레이리스트 공유 알림 전송 실패: receiverId={}, error={}", receiverId, e.getMessage());
            }
        });
    }
    
    /**
     * 추천 시스템 알림 전송
     */
    @Async
    public CompletableFuture<Void> sendRecommendationNotification(Long userId, String recommendationType, 
                                                                 int trackCount) {
        return CompletableFuture.runAsync(() -> {
            try {
                String title = "🎯 새로운 음악 추천";
                String message = switch (recommendationType) {
                    case "personalized" -> String.format("당신을 위한 맞춤 추천 %d곡이 준비되었습니다", trackCount);
                    case "discovery" -> String.format("새로운 장르 탐험을 위한 %d곡을 추천합니다", trackCount);
                    case "trending" -> String.format("지금 인기있는 %d곡을 추천합니다", trackCount);
                    default -> String.format("새로운 추천 %d곡이 도착했습니다", trackCount);
                };
                
                NotificationData notification = createNotification(
                    "NEW_RECOMMENDATIONS",
                    title,
                    message,
                    Map.of(
                        "recommendationType", recommendationType,
                        "trackCount", trackCount,
                        "actionUrl", "/recommendations"
                    )
                );
                
                sendRealTimeNotification(userId, notification);
                saveNotification(userId, notification);
                
                log.debug("추천 알림 전송: userId={}, type={}, count={}", 
                    userId, recommendationType, trackCount);
                    
            } catch (Exception e) {
                log.error("추천 알림 전송 실패: userId={}, error={}", userId, e.getMessage());
            }
        });
    }
    
    /**
     * 사용자별 알림 목록 조회
     */
    public List<NotificationData> getUserNotifications(Long userId, int limit, boolean unreadOnly) {
        List<NotificationData> notifications = userNotifications.getOrDefault(userId, new ArrayList<>());
        
        return notifications.stream()
            .filter(notification -> !unreadOnly || !notification.isRead())
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .toList();
    }
    
    /**
     * 알림 읽음 처리
     */
    public boolean markAsRead(Long userId, String notificationId) {
        List<NotificationData> notifications = userNotifications.get(userId);
        if (notifications == null) return false;
        
        return notifications.stream()
            .filter(n -> n.getId().equals(notificationId))
            .findFirst()
            .map(notification -> {
                notification.setRead(true);
                notification.setReadAt(Instant.now());
                return true;
            })
            .orElse(false);
    }
    
    /**
     * 모든 알림 읽음 처리
     */
    public int markAllAsRead(Long userId) {
        List<NotificationData> notifications = userNotifications.get(userId);
        if (notifications == null) return 0;
        
        int count = 0;
        Instant now = Instant.now();
        
        for (NotificationData notification : notifications) {
            if (!notification.isRead()) {
                notification.setRead(true);
                notification.setReadAt(now);
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 읽지 않은 알림 개수 조회
     */
    public int getUnreadCount(Long userId) {
        List<NotificationData> notifications = userNotifications.get(userId);
        if (notifications == null) return 0;
        
        return (int) notifications.stream()
            .filter(n -> !n.isRead())
            .count();
    }
    
    /**
     * 알림 구독 설정
     */
    public void subscribe(Long userId, String notificationType) {
        userSubscriptions.computeIfAbsent(userId, k -> new HashSet<>()).add(notificationType);
        log.debug("알림 구독: userId={}, type={}", userId, notificationType);
    }
    
    /**
     * 알림 구독 해제
     */
    public void unsubscribe(Long userId, String notificationType) {
        Set<String> subscriptions = userSubscriptions.get(userId);
        if (subscriptions != null) {
            subscriptions.remove(notificationType);
            log.debug("알림 구독 해제: userId={}, type={}", userId, notificationType);
        }
    }
    
    /**
     * 사용자의 구독 설정 조회
     */
    public Set<String> getUserSubscriptions(Long userId) {
        return userSubscriptions.getOrDefault(userId, new HashSet<>());
    }
    
    /**
     * 브로드캐스트 알림 (모든 사용자에게)
     */
    @Async
    public CompletableFuture<Void> broadcastNotification(String title, String message, 
                                                        Map<String, Object> data) {
        return CompletableFuture.runAsync(() -> {
            try {
                NotificationData notification = createNotification(
                    "BROADCAST",
                    title,
                    message,
                    data
                );
                
                // 모든 활성 사용자에게 전송
                messagingTemplate.convertAndSend("/topic/broadcast", notification);
                
                log.info("브로드캐스트 알림 전송: title={}", title);
                
            } catch (Exception e) {
                log.error("브로드캐스트 알림 전송 실패: error={}", e.getMessage());
            }
        });
    }
    
    // Helper Methods
    
    private NotificationData createNotification(String type, String title, String message, 
                                               Map<String, Object> data) {
        return NotificationData.builder()
            .id(UUID.randomUUID().toString())
            .type(type)
            .title(title)
            .message(message)
            .data(data)
            .timestamp(Instant.now())
            .read(false)
            .build();
    }
    
    private void sendRealTimeNotification(Long userId, NotificationData notification) {
        try {
            // 사용자별 개인 알림
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
            );
            
            // 알림 배지 업데이트를 위한 카운트 전송
            int unreadCount = getUnreadCount(userId) + 1; // 새 알림 포함
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notification-count",
                Map.of("unreadCount", unreadCount)
            );
            
        } catch (Exception e) {
            log.warn("실시간 알림 전송 실패: userId={}, error={}", userId, e.getMessage());
        }
    }
    
    private void saveNotification(Long userId, NotificationData notification) {
        userNotifications.computeIfAbsent(userId, k -> new ArrayList<>()).add(notification);
        
        // 알림 개수 제한 (최근 100개만 유지)
        List<NotificationData> notifications = userNotifications.get(userId);
        if (notifications.size() > 100) {
            notifications.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
            userNotifications.put(userId, new ArrayList<>(notifications.subList(0, 100)));
        }
    }
    
    /**
     * 알림 데이터 모델
     */
    public static class NotificationData {
        private String id;
        private String type;
        private String title;
        private String message;
        private Map<String, Object> data;
        private Instant timestamp;
        private boolean read;
        private Instant readAt;
        
        // Builder pattern을 위한 내부 클래스
        public static NotificationDataBuilder builder() {
            return new NotificationDataBuilder();
        }
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
        
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        
        public boolean isRead() { return read; }
        public void setRead(boolean read) { this.read = read; }
        
        public Instant getReadAt() { return readAt; }
        public void setReadAt(Instant readAt) { this.readAt = readAt; }
        
        public static class NotificationDataBuilder {
            private NotificationData notification = new NotificationData();
            
            public NotificationDataBuilder id(String id) {
                notification.id = id;
                return this;
            }
            
            public NotificationDataBuilder type(String type) {
                notification.type = type;
                return this;
            }
            
            public NotificationDataBuilder title(String title) {
                notification.title = title;
                return this;
            }
            
            public NotificationDataBuilder message(String message) {
                notification.message = message;
                return this;
            }
            
            public NotificationDataBuilder data(Map<String, Object> data) {
                notification.data = data;
                return this;
            }
            
            public NotificationDataBuilder timestamp(Instant timestamp) {
                notification.timestamp = timestamp;
                return this;
            }
            
            public NotificationDataBuilder read(boolean read) {
                notification.read = read;
                return this;
            }
            
            public NotificationData build() {
                return notification;
            }
        }
    }
}