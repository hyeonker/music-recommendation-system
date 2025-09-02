package com.example.musicrecommendation;

import com.example.musicrecommendation.domain.UserBadge;
import com.example.musicrecommendation.service.UserBadgeService;
import com.example.musicrecommendation.service.BadgeNotificationService;
import com.example.musicrecommendation.repository.UserBadgeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class BadgeExpirationTest {
    
    @Autowired
    private UserBadgeService userBadgeService;
    
    @Autowired
    private UserBadgeRepository badgeRepository;
    
    @Autowired
    private BadgeNotificationService notificationService;
    
    @Test
    public void testBadgeExpiration() {
        // Given: 과거 시간으로 만료된 배지 생성
        Long testUserId = 999L;
        OffsetDateTime pastTime = OffsetDateTime.now().minusHours(1);
        
        // 만료된 배지 부여
        UserBadge expiredBadge = userBadgeService.awardBadge(
            testUserId, 
            UserBadge.BadgeType.SPECIAL_EVENT, 
            "테스트 이벤트 배지", 
            "테스트용 만료 배지",
            pastTime
        );
        
        // When: 만료된 배지 정리 실행
        int removedCount = userBadgeService.removeExpiredBadges();
        
        // Then: 배지가 제거되었는지 확인
        assertTrue(removedCount > 0, "만료된 배지가 제거되어야 합니다");
        assertFalse(badgeRepository.existsById(expiredBadge.getId()), "만료된 배지는 데이터베이스에서 삭제되어야 합니다");
        
        // 알림이 생성되었는지 확인
        List<BadgeNotificationService.BadgeNotification> notifications = 
            notificationService.getUserNotifications(testUserId);
        
        boolean hasExpiredNotification = notifications.stream()
            .anyMatch(n -> n.getType() == BadgeNotificationService.NotificationType.BADGE_EXPIRED);
        
        assertTrue(hasExpiredNotification, "배지 만료 알림이 생성되어야 합니다");
    }
    
    @Test
    public void testBadgeExpirationNotifications() {
        // Given: 3일 후 만료 예정인 배지 생성
        Long testUserId = 998L;
        OffsetDateTime threeDaysLater = OffsetDateTime.now().plusDays(3);
        
        UserBadge expiringBadge = userBadgeService.awardBadge(
            testUserId,
            UserBadge.BadgeType.ANNIVERSARY,
            "만료 예정 배지",
            "테스트용 만료 예정 배지",
            threeDaysLater
        );
        
        // When: 만료 예정 알림 전송
        userBadgeService.sendExpirationNotifications();
        
        // Then: 알림이 생성되었는지 확인
        List<BadgeNotificationService.BadgeNotification> notifications = 
            notificationService.getUserNotifications(testUserId);
        
        boolean hasExpiringNotification = notifications.stream()
            .anyMatch(n -> n.getType() == BadgeNotificationService.NotificationType.BADGE_EXPIRING);
        
        assertTrue(hasExpiringNotification, "배지 만료 예정 알림이 생성되어야 합니다");
    }
    
    @Test
    public void testBadgeNotExpiredYet() {
        // Given: 미래에 만료되는 배지 생성
        Long testUserId = 997L;
        OffsetDateTime futureTime = OffsetDateTime.now().plusDays(30);
        
        UserBadge futureBadge = userBadgeService.awardBadge(
            testUserId,
            UserBadge.BadgeType.BETA_TESTER,
            "미래 만료 배지",
            "아직 만료되지 않은 배지",
            futureTime
        );
        
        // When: 만료된 배지 정리 실행
        int removedCount = userBadgeService.removeExpiredBadges();
        
        // Then: 아직 만료되지 않은 배지는 유지되어야 함
        assertTrue(badgeRepository.existsById(futureBadge.getId()), "아직 만료되지 않은 배지는 유지되어야 합니다");
        assertFalse(futureBadge.isExpired(), "배지가 아직 만료되지 않았어야 합니다");
    }
}