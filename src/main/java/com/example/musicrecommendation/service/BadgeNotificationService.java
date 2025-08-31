package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.UserBadge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 통합 알림 서비스 (배지, 공지사항, 운영자 메시지 등)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeNotificationService {
    
    // 사용자별 알림 저장소 (실제로는 Redis나 데이터베이스 사용 권장)
    private final ConcurrentHashMap<Long, List<BadgeNotification>> userNotifications = new ConcurrentHashMap<>();
    
    /**
     * 배지 획득 알림 전송
     */
    public void sendBadgeAwardedNotification(Long userId, UserBadge badge) {
        BadgeNotification notification = BadgeNotification.builder()
                .userId(userId)
                .type(NotificationType.BADGE_AWARDED)
                .title("🎉 새 배지 획득!")
                .message(String.format("'%s' 배지를 획득했습니다!", badge.getBadgeName()))
                .badgeId(badge.getId())
                .badgeType(badge.getBadgeType())
                .badgeName(badge.getBadgeName())
                .rarity(badge.getRarity())
                .timestamp(java.time.OffsetDateTime.now(java.time.ZoneId.of("Asia/Seoul")))
                .isRead(false)
                .build();
        
        addNotification(userId, notification);
        
        log.info("🔔 배지 획득 알림 전송 - 사용자: {}, 배지: {}", userId, badge.getBadgeName());
        
        // TODO: WebSocket으로 실시간 알림 전송
        // TODO: 이메일/푸시 알림 전송 (설정에 따라)
    }
    
    /**
     * 배지 만료 예정 알림 전송
     */
    public void sendBadgeExpirationNotification(Long userId, UserBadge badge, long daysUntilExpiry) {
        String message = daysUntilExpiry > 1 ? 
            String.format("'%s' 배지가 %d일 후 만료됩니다.", badge.getBadgeName(), daysUntilExpiry) :
            String.format("'%s' 배지가 내일 만료됩니다!", badge.getBadgeName());
        
        BadgeNotification notification = BadgeNotification.builder()
                .userId(userId)
                .type(NotificationType.BADGE_EXPIRING)
                .title("⏰ 배지 만료 예정")
                .message(message)
                .badgeId(badge.getId())
                .badgeType(badge.getBadgeType())
                .badgeName(badge.getBadgeName())
                .timestamp(java.time.OffsetDateTime.now(java.time.ZoneId.of("Asia/Seoul")))
                .isRead(false)
                .build();
        
        addNotification(userId, notification);
        
        log.info("⏰ 배지 만료 예정 알림 전송 - 사용자: {}, 배지: {}, 남은 일수: {}", 
                userId, badge.getBadgeName(), daysUntilExpiry);
    }
    
    /**
     * 배지 만료 알림 전송
     */
    public void sendBadgeExpiredNotification(Long userId, UserBadge badge) {
        BadgeNotification notification = BadgeNotification.builder()
                .userId(userId)
                .type(NotificationType.BADGE_EXPIRED)
                .title("💔 배지 만료")
                .message(String.format("'%s' 배지가 만료되어 제거되었습니다.", badge.getBadgeName()))
                .badgeId(badge.getId())
                .badgeType(badge.getBadgeType())
                .badgeName(badge.getBadgeName())
                .timestamp(java.time.OffsetDateTime.now(java.time.ZoneId.of("Asia/Seoul")))
                .isRead(false)
                .build();
        
        addNotification(userId, notification);
        
        log.info("💔 배지 만료 알림 전송 - 사용자: {}, 배지: {}", userId, badge.getBadgeName());
    }
    
    /**
     * 사용자의 모든 알림 조회
     */
    public List<BadgeNotification> getUserNotifications(Long userId) {
        List<BadgeNotification> notifications = new ArrayList<>(userNotifications.getOrDefault(userId, new ArrayList<>()));
        log.info("📋 사용자 알림 조회 - userId: {}, 총 알림 수: {}", userId, notifications.size());
        
        // 각 알림의 읽음 상태 로그
        for (BadgeNotification notification : notifications) {
            log.info("📄 알림 상세 - id: {}, title: '{}', isRead: {}, readAt: {}", 
                notification.getId(), notification.getTitle(), notification.isRead(), notification.getReadAt());
        }
        
        return notifications;
    }
    
    /**
     * 사용자의 읽지 않은 알림 개수
     */
    public long getUnreadNotificationCount(Long userId) {
        return userNotifications.getOrDefault(userId, new ArrayList<>())
                .stream()
                .filter(notification -> !notification.isRead())
                .count();
    }
    
    /**
     * 알림 읽음 처리
     */
    public boolean markAsRead(Long userId, Long notificationId) {
        List<BadgeNotification> notifications = userNotifications.get(userId);
        if (notifications != null) {
            java.time.OffsetDateTime now = java.time.OffsetDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
            boolean found = notifications.stream()
                    .filter(n -> n.getId().equals(notificationId))
                    .peek(n -> {
                        log.info("알림 읽음 처리 전 상태 - notificationId: {}, isRead: {}", notificationId, n.isRead());
                        n.setRead(true);
                        n.setReadAt(now);
                        log.info("알림 읽음 처리 후 상태 - notificationId: {}, isRead: {}, readAt: {}", notificationId, n.isRead(), now);
                    })
                    .findAny()
                    .isPresent();
            
            if (found) {
                log.info("✅ 알림 읽음 처리 완료 - userId: {}, notificationId: {}", userId, notificationId);
            } else {
                log.warn("⚠️ 알림을 찾을 수 없음 - userId: {}, notificationId: {}", userId, notificationId);
            }
            return found;
        }
        log.warn("⚠️ 해당 사용자의 알림 목록이 없음 - userId: {}", userId);
        return false;
    }
    
    /**
     * 모든 알림 읽음 처리
     */
    public void markAllAsRead(Long userId) {
        List<BadgeNotification> notifications = userNotifications.get(userId);
        if (notifications != null) {
            java.time.OffsetDateTime now = java.time.OffsetDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
            int readCount = 0;
            for (BadgeNotification n : notifications) {
                if (!n.isRead()) {
                    n.setRead(true);
                    n.setReadAt(now);
                    readCount++;
                }
            }
            log.info("모든 알림 읽음 처리 완료 - userId: {}, 읽음 처리된 알림 수: {}", userId, readCount);
        }
    }
    
    /**
     * 알림 삭제
     */
    public void deleteNotification(Long userId, Long notificationId) {
        List<BadgeNotification> notifications = userNotifications.get(userId);
        if (notifications != null) {
            notifications.removeIf(n -> n.getId().equals(notificationId));
        }
    }
    
    private void addNotification(Long userId, BadgeNotification notification) {
        userNotifications.computeIfAbsent(userId, k -> new ArrayList<>()).add(notification);
        
        // 최대 100개의 알림만 유지 (오래된 것부터 삭제)
        List<BadgeNotification> notifications = userNotifications.get(userId);
        if (notifications.size() > 100) {
            notifications.subList(0, notifications.size() - 100).clear();
        }
    }
    
    /**
     * 배지 알림 클래스
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    public static class BadgeNotification {
        private Long id;
        private Long userId;
        private NotificationType type;
        private String title;
        private String message;
        private Long badgeId;
        private UserBadge.BadgeType badgeType;
        private String badgeName;
        private String rarity;
        private java.time.OffsetDateTime timestamp;
        @com.fasterxml.jackson.annotation.JsonProperty("isRead")
        private boolean isRead;
        private java.time.OffsetDateTime readAt;
        
        // Builder 패턴을 위한 생성자
        @lombok.Builder
        public BadgeNotification(Long userId, NotificationType type, String title, String message,
                               Long badgeId, UserBadge.BadgeType badgeType, String badgeName, 
                               String rarity, java.time.OffsetDateTime timestamp, boolean isRead) {
            this.id = System.currentTimeMillis() + (long) (Math.random() * 1000); // 고유 ID 생성
            this.userId = userId;
            this.type = type;
            this.title = title;
            this.message = message;
            this.badgeId = badgeId;
            this.badgeType = badgeType;
            this.badgeName = badgeName;
            this.rarity = rarity;
            this.timestamp = timestamp;
            this.isRead = isRead;
        }
    }
    
    /**
     * 알림 타입 (확장 가능 - 운영자 공지사항, 시스템 알림 등 추가 가능)
     */
    public enum NotificationType {
        // 배지 관련 알림
        BADGE_AWARDED("배지 획득"),
        BADGE_EXPIRING("배지 만료 예정"),
        BADGE_EXPIRED("배지 만료"),
        
        // 시스템 알림 타입들
        SYSTEM_ANNOUNCEMENT("시스템 공지"),
        ADMIN_MESSAGE("운영자 메시지"),
        UPDATE_NOTIFICATION("업데이트 알림"),
        MAINTENANCE_NOTICE("점검 안내"),
        EVENT_NOTIFICATION("이벤트 알림");
        
        private final String description;
        
        NotificationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 사용자에게 직접 알림 추가 (시스템 알림용)
     */
    public void addNotificationForUser(Long userId, BadgeNotification notification) {
        // 알림 ID 생성 (시간 기반)
        if (notification.getId() == null) {
            notification.setId(System.currentTimeMillis() + userId); // 고유 ID 생성
        }
        
        userNotifications.computeIfAbsent(userId, k -> new ArrayList<>()).add(notification);
        log.info("📨 사용자 {} 에게 시스템 알림 추가 - 제목: '{}'", userId, notification.getTitle());
    }
    
    /**
     * 알림 삭제
     */
    public void removeNotification(Long userId, Long notificationId) {
        List<BadgeNotification> notifications = userNotifications.get(userId);
        if (notifications != null) {
            notifications.removeIf(n -> n.getId().equals(notificationId));
            log.info("🗑️ 사용자 {} 의 알림 삭제 - 알림 ID: {}", userId, notificationId);
        }
    }
}