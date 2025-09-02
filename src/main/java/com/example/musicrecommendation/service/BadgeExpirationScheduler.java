package com.example.musicrecommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 배지 만료 스케줄러
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeExpirationScheduler {
    
    private final UserBadgeService userBadgeService;
    private final Environment environment;
    
    /**
     * 매일 자정에 만료된 배지를 자동으로 제거
     */
    @Scheduled(cron = "0 0 0 * * ?") // 매일 00:00:00
    public void removeExpiredBadges() {
        log.info("🕒 배지 만료 스케줄러 시작 - 만료된 배지 자동 제거");
        try {
            int removedCount = userBadgeService.removeExpiredBadges();
            log.info("✅ 배지 만료 스케줄러 완료 - 제거된 배지 수: {}", removedCount);
        } catch (Exception e) {
            log.error("❌ 배지 만료 스케줄러 실행 중 오류 발생", e);
        }
    }
    
    /**
     * 매일 오전 9시에 만료 예정 배지 알림 전송 (3일, 1일 전)
     */
    @Scheduled(cron = "0 0 9 * * ?") // 매일 09:00:00
    public void sendExpirationWarnings() {
        log.info("🔔 배지 만료 예정 알림 스케줄러 시작");
        try {
            userBadgeService.sendExpirationNotifications();
            log.info("✅ 배지 만료 예정 알림 스케줄러 완료");
        } catch (Exception e) {
            log.error("❌ 배지 만료 예정 알림 스케줄러 실행 중 오류 발생", e);
        }
    }
    
}