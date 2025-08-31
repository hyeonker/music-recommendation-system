package com.example.musicrecommendation.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 시스템 알림 관련 DTO 클래스들
 */
public class SystemNotificationDto {
    
    /**
     * 시스템 알림 생성 요청 DTO
     */
    @Schema(description = "시스템 알림 생성 요청")
    public static class CreateRequest {
        @Schema(description = "알림 유형", example = "SYSTEM_ANNOUNCEMENT")
        private String type;
        
        @Schema(description = "알림 제목", example = "시스템 점검 안내")
        private String title;
        
        @Schema(description = "알림 내용", example = "2024년 1월 1일 오전 2시부터 4시까지 시스템 점검이 있습니다.")
        private String message;
        
        @Schema(description = "발송 대상 유형", example = "ALL")
        private String targetType;
        
        @Schema(description = "특정 사용자 ID 목록 (targetType이 SPECIFIC일 때만 사용)")
        private List<Long> targetUserIds;
        
        @Schema(description = "우선순위", example = "HIGH")
        private String priority;
        
        @Schema(description = "예약 발송 시간 (즉시 발송이면 null)")
        private OffsetDateTime scheduledAt;
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getTargetType() { return targetType; }
        public void setTargetType(String targetType) { this.targetType = targetType; }
        
        public List<Long> getTargetUserIds() { return targetUserIds; }
        public void setTargetUserIds(List<Long> targetUserIds) { this.targetUserIds = targetUserIds; }
        
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        
        public OffsetDateTime getScheduledAt() { return scheduledAt; }
        public void setScheduledAt(OffsetDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    }
    
    /**
     * 시스템 알림 응답 DTO
     */
    @Schema(description = "시스템 알림 응답")
    public static class Response {
        @Schema(description = "알림 ID")
        private Long id;
        
        @Schema(description = "알림 유형")
        private String type;
        
        @Schema(description = "알림 제목")
        private String title;
        
        @Schema(description = "알림 내용")
        private String message;
        
        @Schema(description = "발송 대상 유형")
        private String targetType;
        
        @Schema(description = "대상 사용자 수")
        private Integer targetUserCount;
        
        @Schema(description = "우선순위")
        private String priority;
        
        @Schema(description = "알림 상태")
        private String status;
        
        @Schema(description = "생성 시간")
        private OffsetDateTime createdAt;
        
        @Schema(description = "예약 발송 시간")
        private OffsetDateTime scheduledAt;
        
        @Schema(description = "실제 발송 시간")
        private OffsetDateTime sentAt;
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getTargetType() { return targetType; }
        public void setTargetType(String targetType) { this.targetType = targetType; }
        
        public Integer getTargetUserCount() { return targetUserCount; }
        public void setTargetUserCount(Integer targetUserCount) { this.targetUserCount = targetUserCount; }
        
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        
        public OffsetDateTime getScheduledAt() { return scheduledAt; }
        public void setScheduledAt(OffsetDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
        
        public OffsetDateTime getSentAt() { return sentAt; }
        public void setSentAt(OffsetDateTime sentAt) { this.sentAt = sentAt; }
    }
    
    /**
     * 알림 통계 응답 DTO
     */
    @Schema(description = "시스템 알림 통계")
    public static class StatsResponse {
        @Schema(description = "시스템 공지사항 수")
        private long systemAnnouncementCount;
        
        @Schema(description = "운영자 메시지 수")
        private long adminMessageCount;
        
        @Schema(description = "업데이트 알림 수")
        private long updateNotificationCount;
        
        @Schema(description = "점검 안내 수")
        private long maintenanceNoticeCount;
        
        @Schema(description = "이벤트 알림 수")
        private long eventNotificationCount;
        
        // Getters and Setters
        public long getSystemAnnouncementCount() { return systemAnnouncementCount; }
        public void setSystemAnnouncementCount(long systemAnnouncementCount) { this.systemAnnouncementCount = systemAnnouncementCount; }
        
        public long getAdminMessageCount() { return adminMessageCount; }
        public void setAdminMessageCount(long adminMessageCount) { this.adminMessageCount = adminMessageCount; }
        
        public long getUpdateNotificationCount() { return updateNotificationCount; }
        public void setUpdateNotificationCount(long updateNotificationCount) { this.updateNotificationCount = updateNotificationCount; }
        
        public long getMaintenanceNoticeCount() { return maintenanceNoticeCount; }
        public void setMaintenanceNoticeCount(long maintenanceNoticeCount) { this.maintenanceNoticeCount = maintenanceNoticeCount; }
        
        public long getEventNotificationCount() { return eventNotificationCount; }
        public void setEventNotificationCount(long eventNotificationCount) { this.eventNotificationCount = eventNotificationCount; }
    }
}