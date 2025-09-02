package com.example.musicrecommendation.config;

import com.example.musicrecommendation.service.SecureChatRoomService;
import com.example.musicrecommendation.service.ChatRateLimitService;
import com.example.musicrecommendation.service.ChatExpiryNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 채팅방 정리 스케줄러
 * 만료된 채팅방을 정기적으로 정리하여 메모리 사용량을 최적화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomCleanupScheduler {
    
    private final SecureChatRoomService secureChatRoomService;
    private final ChatRateLimitService rateLimitService;
    private final ChatExpiryNotificationService expiryNotificationService;
    
    /**
     * 매시간마다 만료된 채팅방 정리
     * cron 표현식: 0 0 * * * * (매시간 정각)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredChatRooms() {
        try {
            log.info("만료된 채팅방 정리 작업 시작");
            
            int cleanedCount = secureChatRoomService.cleanupExpiredRooms();
            
            // 만료된 경고 플래그 정리
            expiryNotificationService.cleanupExpiredWarnings();
            
            if (cleanedCount > 0) {
                log.info("채팅방 정리 작업 완료: {}개 방 삭제", cleanedCount);
            } else {
                log.debug("정리할 만료된 채팅방이 없음");
            }
            
        } catch (Exception e) {
            log.error("채팅방 정리 작업 중 오류 발생", e);
        }
    }
    
    /**
     * 매분마다 채팅방 만료 경고 확인 및 전송
     * cron 표현식: 0 * * * * * (매분마다)
     */
    @Scheduled(cron = "0 * * * * *")
    public void sendExpiryWarnings() {
        try {
            expiryNotificationService.sendExpiryWarnings();
        } catch (Exception e) {
            log.error("만료 경고 전송 중 오류 발생", e);
        }
    }
    
    /**
     * 30분마다 채팅방 통계 로그 출력 (디버깅용)
     * cron 표현식: 0 0/30 * * * * (매 30분마다)
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void logChatRoomStats() {
        try {
            // Rate Limit 카운터 정리
            rateLimitService.cleanup();
            log.debug("채팅방 통계 수집 및 Rate Limit 정리 완료");
        } catch (Exception e) {
            log.warn("채팅방 통계 수집 중 오류 발생", e);
        }
    }
}