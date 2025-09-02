package com.example.musicrecommendation.event;

import com.example.musicrecommendation.service.UserBadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class BadgeEventListener {
    
    private final UserBadgeService userBadgeService;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBadgeEvent(BadgeEvent event) {
        log.info("🔄 배지 이벤트 처리 시작 - userId: {}, action: {}, badge: {}", 
                event.getUserId(), event.getAction(), event.getBadgeName());
        
        try {
            // 트랜잭션 완료 후 캐시 무효화
            userBadgeService.evictUserBadgeCache(event.getUserId());
            log.info("✅ 배지 캐시 무효화 완료 - userId: {}", event.getUserId());
        } catch (Exception e) {
            log.error("❌ 배지 캐시 무효화 실패 - userId: {}", event.getUserId(), e);
        }
    }
}