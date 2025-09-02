package com.example.musicrecommendation;

import com.example.musicrecommendation.service.BadgeNotificationService;
import com.example.musicrecommendation.domain.UserBadge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class SimpleNotificationTest {
    
    @Autowired
    private BadgeNotificationService notificationService;
    
    @Test
    public void testNotificationServiceBasicOperations() {
        // Given: 테스트 사용자와 가짜 배지
        Long testUserId = 9999L;
        
        // 가짜 배지 객체 생성
        UserBadge fakeBadge = UserBadge.builder()
            .id(1L)
            .userId(testUserId)
            .badgeType(UserBadge.BadgeType.FIRST_REVIEW)
            .badgeName("테스트 배지")
            .description("테스트용 배지")
            .iconUrl("test-icon")
            .earnedAt(OffsetDateTime.now())
            .build();
        
        // When: 배지 획득 알림 전송
        notificationService.sendBadgeAwardedNotification(testUserId, fakeBadge);
        
        // Then: 알림이 생성되었는지 확인
        List<BadgeNotificationService.BadgeNotification> notifications = 
            notificationService.getUserNotifications(testUserId);
        
        assertFalse(notifications.isEmpty(), "알림이 생성되어야 합니다");
        
        BadgeNotificationService.BadgeNotification notification = notifications.get(0);
        assertEquals(testUserId, notification.getUserId(), "사용자 ID가 일치해야 합니다");
        assertEquals(BadgeNotificationService.NotificationType.BADGE_AWARDED, 
            notification.getType(), "알림 타입이 BADGE_AWARDED여야 합니다");
        assertEquals("🎉 새 배지 획득!", notification.getTitle(), "제목이 일치해야 합니다");
        assertTrue(notification.getMessage().contains("테스트 배지"), 
            "메시지에 배지 이름이 포함되어야 합니다");
        assertFalse(notification.isRead(), "새 알림은 읽지 않은 상태여야 합니다");
        
        // 읽지 않은 알림 개수 확인
        long unreadCount = notificationService.getUnreadNotificationCount(testUserId);
        assertEquals(1L, unreadCount, "읽지 않은 알림이 1개여야 합니다");
    }
    
    @Test
    public void testNotificationReadAndDelete() {
        // Given: 알림 생성
        Long testUserId = 9998L;
        
        UserBadge fakeBadge = UserBadge.builder()
            .id(2L)
            .userId(testUserId)
            .badgeType(UserBadge.BadgeType.SOCIAL_BUTTERFLY)
            .badgeName("소셜 배지")
            .description("소셜 활동 배지")
            .iconUrl("social-icon")
            .earnedAt(OffsetDateTime.now())
            .build();
        
        notificationService.sendBadgeAwardedNotification(testUserId, fakeBadge);
        
        List<BadgeNotificationService.BadgeNotification> notifications = 
            notificationService.getUserNotifications(testUserId);
        
        assertEquals(1, notifications.size(), "알림이 1개 생성되어야 합니다");
        
        BadgeNotificationService.BadgeNotification notification = notifications.get(0);
        
        // When: 알림 읽음 처리
        notificationService.markAsRead(testUserId, notification.getId());
        
        // Then: 알림이 읽음 상태로 변경
        List<BadgeNotificationService.BadgeNotification> updatedNotifications = 
            notificationService.getUserNotifications(testUserId);
        
        assertEquals(1, updatedNotifications.size(), "알림이 여전히 1개여야 합니다");
        assertTrue(updatedNotifications.get(0).isRead(), "알림이 읽음 상태여야 합니다");
        
        long unreadCount = notificationService.getUnreadNotificationCount(testUserId);
        assertEquals(0L, unreadCount, "읽지 않은 알림이 0개여야 합니다");
        
        // When: 알림 삭제
        notificationService.deleteNotification(testUserId, notification.getId());
        
        // Then: 알림이 삭제됨
        List<BadgeNotificationService.BadgeNotification> finalNotifications = 
            notificationService.getUserNotifications(testUserId);
        
        assertTrue(finalNotifications.isEmpty(), "알림이 삭제되어야 합니다");
    }
    
    @Test
    public void testMultipleNotificationTypes() {
        // Given: 테스트 사용자와 여러 타입의 알림
        Long testUserId = 9997L;
        
        UserBadge testBadge = UserBadge.builder()
            .id(3L)
            .userId(testUserId)
            .badgeType(UserBadge.BadgeType.ANNIVERSARY)
            .badgeName("기념일 배지")
            .description("1주년 기념 배지")
            .iconUrl("anniversary-icon")
            .earnedAt(OffsetDateTime.now())
            .expiresAt(OffsetDateTime.now().plusDays(7))
            .build();
        
        // When: 여러 타입의 알림 전송
        notificationService.sendBadgeAwardedNotification(testUserId, testBadge);
        notificationService.sendBadgeExpirationNotification(testUserId, testBadge, 3L);
        
        // Then: 각 타입의 알림이 생성됨
        List<BadgeNotificationService.BadgeNotification> notifications = 
            notificationService.getUserNotifications(testUserId);
        
        assertEquals(2, notifications.size(), "2개의 알림이 생성되어야 합니다");
        
        boolean hasAwardedNotification = notifications.stream()
            .anyMatch(n -> n.getType() == BadgeNotificationService.NotificationType.BADGE_AWARDED);
        boolean hasExpiringNotification = notifications.stream()
            .anyMatch(n -> n.getType() == BadgeNotificationService.NotificationType.BADGE_EXPIRING);
        
        assertTrue(hasAwardedNotification, "배지 획득 알림이 있어야 합니다");
        assertTrue(hasExpiringNotification, "배지 만료 예정 알림이 있어야 합니다");
        
        // 모든 알림 읽음 처리
        notificationService.markAllAsRead(testUserId);
        
        long unreadCount = notificationService.getUnreadNotificationCount(testUserId);
        assertEquals(0L, unreadCount, "모든 알림이 읽음 상태여야 합니다");
    }
}