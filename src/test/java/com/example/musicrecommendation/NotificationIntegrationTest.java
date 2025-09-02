package com.example.musicrecommendation;

import com.example.musicrecommendation.domain.UserBadge;
import com.example.musicrecommendation.service.UserBadgeService;
import com.example.musicrecommendation.service.BadgeNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class NotificationIntegrationTest {
    
    @Autowired
    private UserBadgeService userBadgeService;
    
    @Autowired
    private BadgeNotificationService notificationService;
    
    @Test
    public void testBadgeAwardNotificationFlow() {
        // Given: 테스트 사용자 ID
        Long testUserId = 1001L;
        
        // 알림이 없는 상태인지 확인
        long initialNotificationCount = notificationService.getUnreadNotificationCount(testUserId);
        
        // When: 새 배지 부여 (알림 전송 포함)
        UserBadge newBadge = userBadgeService.awardBadge(
            testUserId,
            UserBadge.BadgeType.FIRST_REVIEW,
            "첫 리뷰 작성자",
            "첫 번째 리뷰를 작성한 사용자"
        );
        
        // Then: 배지가 정상적으로 생성되었는지 확인
        assertNotNull(newBadge, "배지가 생성되어야 합니다");
        assertEquals(testUserId, newBadge.getUserId(), "배지의 사용자 ID가 일치해야 합니다");
        
        // 알림이 생성되었는지 확인
        long finalNotificationCount = notificationService.getUnreadNotificationCount(testUserId);
        assertTrue(finalNotificationCount > initialNotificationCount, "새로운 알림이 생성되어야 합니다");
        
        // 알림 내용 확인
        List<BadgeNotificationService.BadgeNotification> notifications = 
            notificationService.getUserNotifications(testUserId);
        
        boolean hasAwardedNotification = notifications.stream()
            .anyMatch(n -> n.getType() == BadgeNotificationService.NotificationType.BADGE_AWARDED);
        
        assertTrue(hasAwardedNotification, "배지 획득 알림이 존재해야 합니다");
        
        // 특정 알림의 세부 정보 확인
        BadgeNotificationService.BadgeNotification awardedNotification = notifications.stream()
            .filter(n -> n.getType() == BadgeNotificationService.NotificationType.BADGE_AWARDED)
            .findFirst()
            .orElse(null);
        
        assertNotNull(awardedNotification, "배지 획득 알림을 찾을 수 있어야 합니다");
        assertEquals(testUserId, awardedNotification.getUserId(), "알림의 사용자 ID가 일치해야 합니다");
        assertEquals("🎉 새 배지 획득!", awardedNotification.getTitle(), "알림 제목이 일치해야 합니다");
        assertTrue(awardedNotification.getMessage().contains("첫 리뷰 작성자"), "알림 메시지에 배지 이름이 포함되어야 합니다");
        assertFalse(awardedNotification.isRead(), "새 알림은 읽지 않은 상태여야 합니다");
    }
    
    @Test
    public void testNotificationReadAndDelete() {
        // Given: 배지 부여로 알림 생성
        Long testUserId = 1002L;
        
        UserBadge testBadge = userBadgeService.awardBadge(
            testUserId,
            UserBadge.BadgeType.SOCIAL_BUTTERFLY,
            "테스트 배지",
            "테스트용 배지"
        );
        
        List<BadgeNotificationService.BadgeNotification> initialNotifications = 
            notificationService.getUserNotifications(testUserId);
        
        assertFalse(initialNotifications.isEmpty(), "알림이 생성되어야 합니다");
        
        BadgeNotificationService.BadgeNotification notification = initialNotifications.get(0);
        assertFalse(notification.isRead(), "초기 알림은 읽지 않은 상태여야 합니다");
        
        // When: 알림 읽음 처리
        notificationService.markAsRead(testUserId, notification.getId());
        
        // Then: 알림이 읽음 상태로 변경되었는지 확인
        List<BadgeNotificationService.BadgeNotification> updatedNotifications = 
            notificationService.getUserNotifications(testUserId);
        
        BadgeNotificationService.BadgeNotification updatedNotification = updatedNotifications.stream()
            .filter(n -> n.getId().equals(notification.getId()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(updatedNotification, "알림이 여전히 존재해야 합니다");
        assertTrue(updatedNotification.isRead(), "알림이 읽음 상태로 변경되어야 합니다");
        
        // When: 알림 삭제
        notificationService.deleteNotification(testUserId, notification.getId());
        
        // Then: 알림이 삭제되었는지 확인
        List<BadgeNotificationService.BadgeNotification> finalNotifications = 
            notificationService.getUserNotifications(testUserId);
        
        boolean notificationExists = finalNotifications.stream()
            .anyMatch(n -> n.getId().equals(notification.getId()));
        
        assertFalse(notificationExists, "알림이 삭제되어야 합니다");
    }
    
    @Test
    public void testBatchNotificationFlow() {
        // Given: 여러 사용자에게 배치 배지 부여
        List<Long> userIds = List.of(1003L, 1004L, 1005L);
        
        // When: 배치 배지 부여
        UserBadgeService.BatchAwardResult result = userBadgeService.batchAwardBadge(
            userIds,
            UserBadge.BadgeType.BETA_TESTER,
            "베타 테스터",
            "베타 테스트에 참여해주신 분들께 드리는 배지",
            null // 만료일 없음
        );
        
        // Then: 배치 부여 결과 확인
        assertTrue(result.getSuccessCount() > 0, "성공한 배지 부여가 있어야 합니다");
        
        // 각 사용자별 알림 생성 확인
        for (Long userId : userIds) {
            List<BadgeNotificationService.BadgeNotification> notifications = 
                notificationService.getUserNotifications(userId);
            
            boolean hasBetaTesterNotification = notifications.stream()
                .anyMatch(n -> n.getType() == BadgeNotificationService.NotificationType.BADGE_AWARDED
                    && n.getBadgeType().toString().equals("BETA_TESTER"));
            
            assertTrue(hasBetaTesterNotification, 
                String.format("사용자 %d는 베타 테스터 배지 획득 알림을 받아야 합니다", userId));
        }
    }
}