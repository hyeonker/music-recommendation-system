package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 시스템 알림 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemNotificationService {
    
    private final SystemNotificationRepository systemNotificationRepository;
    private final UserRepository userRepository;
    private final BadgeNotificationService badgeNotificationService;
    private final ObjectMapper objectMapper;
    
    /**
     * 시스템 알림 생성 및 발송
     */
    @Transactional
    public SystemNotificationSendResult createAndSendNotification(SystemNotificationCreateRequest request) {
        log.info("🔔 시스템 알림 생성 및 발송 시작 - 유형: {}, 제목: '{}'", request.getType(), request.getTitle());
        
        // 1. 시스템 알림 엔티티 생성
        SystemNotification notification = new SystemNotification();
        notification.setType(request.getType());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setTargetType(request.getTargetType());
        notification.setPriority(request.getPriority());
        
        // 2. 대상 사용자 ID 처리
        if (request.getTargetType() == SystemNotification.TargetType.SPECIFIC 
            && request.getTargetUserIds() != null && !request.getTargetUserIds().isEmpty()) {
            try {
                notification.setTargetUserIds(objectMapper.writeValueAsString(request.getTargetUserIds()));
            } catch (JsonProcessingException e) {
                log.error("사용자 ID 목록 직렬화 실패", e);
                throw new RuntimeException("사용자 ID 목록 처리 중 오류가 발생했습니다.", e);
            }
        }
        
        // 3. 예약 발송 처리
        if (request.getScheduledAt() != null) {
            notification.setScheduledAt(request.getScheduledAt());
            notification.setStatus(SystemNotification.NotificationStatus.SCHEDULED);
            log.info("⏰ 예약 발송으로 설정 - 발송 예정 시간: {}", request.getScheduledAt());
        } else {
            notification.setStatus(SystemNotification.NotificationStatus.PENDING);
        }
        
        // 4. 데이터베이스에 저장
        SystemNotification savedNotification = systemNotificationRepository.save(notification);
        log.info("💾 시스템 알림 저장 완료 - ID: {}", savedNotification.getId());
        
        // 5. 즉시 발송이면 바로 발송 처리
        log.info("🔍 발송 유형 확인 - scheduledAt: {}, isImmediateSend: {}", 
                savedNotification.getScheduledAt(), savedNotification.isImmediateSend());
        
        if (savedNotification.isImmediateSend()) {
            log.info("⚡ 즉시 발송 모드로 진입");
            return sendNotificationNow(savedNotification);
        } else {
            log.info("📅 예약 발송 모드로 진입");
            // 예약 발송은 스케줄러가 처리
            SystemNotificationSendResult result = new SystemNotificationSendResult();
            result.setNotificationId(savedNotification.getId());
            result.setScheduledAt(savedNotification.getScheduledAt());
            result.setSuccessCount(0);
            result.setFailCount(0);
            result.setMessage("알림이 예약되었습니다. 예정 시간에 자동으로 발송됩니다.");
            log.info("📅 알림 예약 완료 - ID: {}, 발송 예정: {}", savedNotification.getId(), savedNotification.getScheduledAt());
            return result;
        }
    }
    
    /**
     * 알림 즉시 발송
     */
    @Transactional
    public SystemNotificationSendResult sendNotificationNow(SystemNotification notification) {
        log.info("🚀 알림 즉시 발송 시작 - ID: {}, 유형: {}", notification.getId(), notification.getType());
        
        // 상태를 발송중으로 변경
        notification.setStatus(SystemNotification.NotificationStatus.SENDING);
        systemNotificationRepository.save(notification);
        
        try {
            // 대상 사용자 목록 조회
            List<User> targetUsers = getTargetUsers(notification);
            log.info("👥 발송 대상 사용자 수: {}명", targetUsers.size());
            
            // 각 사용자에게 알림 발송
            int successCount = 0;
            int failCount = 0;
            List<String> results = new ArrayList<>();
            
            for (User user : targetUsers) {
                try {
                    sendNotificationToUser(user, notification);
                    successCount++;
                    results.add("사용자 ID " + user.getId() + ": 발송 성공");
                    log.debug("✅ 사용자 {}에게 알림 발송 성공", user.getId());
                } catch (Exception e) {
                    failCount++;
                    results.add("사용자 ID " + user.getId() + ": 발송 실패 - " + e.getMessage());
                    log.error("❌ 사용자 {}에게 알림 발송 실패", user.getId(), e);
                }
            }
            
            // 발송 완료 처리
            notification.setStatus(SystemNotification.NotificationStatus.SENT);
            notification.setSentAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
            systemNotificationRepository.save(notification);
            
            // 결과 반환
            SystemNotificationSendResult result = new SystemNotificationSendResult();
            result.setNotificationId(notification.getId());
            result.setSuccessCount(successCount);
            result.setFailCount(failCount);
            result.setResults(results);
            result.setMessage(String.format("알림 발송 완료 - 성공: %d명, 실패: %d명", successCount, failCount));
            
            log.info("🎉 알림 발송 완료 - ID: {}, 성공: {}명, 실패: {}명", notification.getId(), successCount, failCount);
            return result;
            
        } catch (Exception e) {
            // 발송 실패 처리
            notification.setStatus(SystemNotification.NotificationStatus.FAILED);
            systemNotificationRepository.save(notification);
            log.error("💥 알림 발송 중 전체 실패 - ID: {}", notification.getId(), e);
            throw new RuntimeException("알림 발송 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 개별 사용자에게 알림 발송
     */
    private void sendNotificationToUser(User user, SystemNotification notification) {
        log.info("📤 사용자 {}({})에게 {} 알림 발송 시작", user.getId(), user.getName(), notification.getType().getDisplayName());
        
        // BadgeNotificationService를 활용해서 실제 알림 생성
        BadgeNotificationService.BadgeNotification userNotification = BadgeNotificationService.BadgeNotification.builder()
                .userId(user.getId())
                .type(convertToNotificationType(notification.getType()))
                .title(notification.getTitle())
                .message(notification.getMessage())
                .badgeId(null) // 시스템 알림은 배지와 무관
                .badgeType(null)
                .badgeName(notification.getType().getDisplayName())
                .rarity(notification.getPriority().getDisplayName())
                .timestamp(OffsetDateTime.now(ZoneId.of("Asia/Seoul")))
                .isRead(false)
                .build();
        
        // 사용자별 알림 저장
        badgeNotificationService.addNotificationForUser(user.getId(), userNotification);
        
        log.info("✉️ 사용자 {}({}) 알림 저장 완료 - 제목: '{}'", user.getId(), user.getName(), notification.getTitle());
    }
    
    /**
     * SystemNotification.NotificationType을 BadgeNotificationService.NotificationType으로 변환
     */
    private BadgeNotificationService.NotificationType convertToNotificationType(SystemNotification.NotificationType type) {
        // 시스템 알림 타입을 그대로 매핑
        switch (type) {
            case SYSTEM_ANNOUNCEMENT:
                return BadgeNotificationService.NotificationType.SYSTEM_ANNOUNCEMENT;
            case ADMIN_MESSAGE:
                return BadgeNotificationService.NotificationType.ADMIN_MESSAGE;
            case UPDATE_NOTIFICATION:
                return BadgeNotificationService.NotificationType.UPDATE_NOTIFICATION;
            case MAINTENANCE_NOTICE:
                return BadgeNotificationService.NotificationType.MAINTENANCE_NOTICE;
            case EVENT_NOTIFICATION:
                return BadgeNotificationService.NotificationType.EVENT_NOTIFICATION;
            default:
                return BadgeNotificationService.NotificationType.BADGE_AWARDED;
        }
    }
    
    /**
     * 대상 사용자 목록 조회
     */
    private List<User> getTargetUsers(SystemNotification notification) {
        if (notification.getTargetType() == SystemNotification.TargetType.ALL) {
            // 전체 사용자
            List<User> allUsers = userRepository.findAll();
            log.debug("🌐 전체 사용자 대상 - 총 {}명", allUsers.size());
            return allUsers;
        } else {
            // 특정 사용자들
            try {
                List<Long> userIds = objectMapper.readValue(
                    notification.getTargetUserIds(), 
                    new TypeReference<List<Long>>() {}
                );
                List<User> specificUsers = userRepository.findAllById(userIds);
                log.info("🎯 특정 사용자 대상 - 요청 ID: {}, 실제 조회: {}명", userIds, specificUsers.size());
                specificUsers.forEach(user -> log.info("  - 대상 사용자: ID={}, 이름={}", user.getId(), user.getName()));
                return specificUsers;
            } catch (JsonProcessingException e) {
                log.error("사용자 ID 목록 역직렬화 실패", e);
                throw new RuntimeException("사용자 ID 목록 처리 중 오류가 발생했습니다.", e);
            }
        }
    }
    
    /**
     * 예약된 알림들을 주기적으로 확인하여 발송 (매분 실행)
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    public void processScheduledNotifications() {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        List<SystemNotification> scheduledNotifications = 
            systemNotificationRepository.findScheduledNotificationsToSend(now);
        
        if (!scheduledNotifications.isEmpty()) {
            log.info("📅 예약 알림 처리 시작 - {} 개의 알림 발송 예정", scheduledNotifications.size());
            
            for (SystemNotification notification : scheduledNotifications) {
                try {
                    log.info("⏰ 예약 알림 발송 시작 - ID: {}, 예정 시간: {}", 
                        notification.getId(), notification.getScheduledAt());
                    sendNotificationNow(notification);
                } catch (Exception e) {
                    log.error("❌ 예약 알림 발송 실패 - ID: {}", notification.getId(), e);
                }
            }
        }
    }
    
    /**
     * 시스템 알림 목록 조회
     */
    @Transactional(readOnly = true)
    public List<SystemNotification> getAllNotifications() {
        return systemNotificationRepository.findAllByOrderByCreatedAtDesc();
    }
    
    /**
     * 최근 30일간의 알림 목록 조회
     */
    @Transactional(readOnly = true)
    public List<SystemNotification> getRecentNotifications() {
        OffsetDateTime thirtyDaysAgo = OffsetDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(30);
        return systemNotificationRepository.findRecentNotifications(thirtyDaysAgo);
    }
    
    /**
     * 특정 유형의 알림 개수 조회
     */
    @Transactional(readOnly = true)
    public long getNotificationCountByType(SystemNotification.NotificationType type) {
        return systemNotificationRepository.countByType(type);
    }
    
    /**
     * 알림 생성 요청 DTO
     */
    public static class SystemNotificationCreateRequest {
        private SystemNotification.NotificationType type;
        private String title;
        private String message;
        private SystemNotification.TargetType targetType;
        private List<Long> targetUserIds;
        private SystemNotification.Priority priority;
        private OffsetDateTime scheduledAt;
        
        // Getters and Setters
        public SystemNotification.NotificationType getType() { return type; }
        public void setType(SystemNotification.NotificationType type) { this.type = type; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public SystemNotification.TargetType getTargetType() { return targetType; }
        public void setTargetType(SystemNotification.TargetType targetType) { this.targetType = targetType; }
        
        public List<Long> getTargetUserIds() { return targetUserIds; }
        public void setTargetUserIds(List<Long> targetUserIds) { this.targetUserIds = targetUserIds; }
        
        public SystemNotification.Priority getPriority() { return priority; }
        public void setPriority(SystemNotification.Priority priority) { this.priority = priority; }
        
        public OffsetDateTime getScheduledAt() { return scheduledAt; }
        public void setScheduledAt(OffsetDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    }
    
    /**
     * 알림 발송 결과 DTO
     */
    public static class SystemNotificationSendResult {
        private Long notificationId;
        private int successCount;
        private int failCount;
        private List<String> results;
        private String message;
        private OffsetDateTime scheduledAt;
        
        // Getters and Setters
        public Long getNotificationId() { return notificationId; }
        public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getFailCount() { return failCount; }
        public void setFailCount(int failCount) { this.failCount = failCount; }
        
        public List<String> getResults() { return results; }
        public void setResults(List<String> results) { this.results = results; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public OffsetDateTime getScheduledAt() { return scheduledAt; }
        public void setScheduledAt(OffsetDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    }
}