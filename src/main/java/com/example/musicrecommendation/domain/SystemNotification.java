package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 시스템 알림 엔티티
 */
@Entity
@Table(name = "system_notifications")
@Getter @Setter
@NoArgsConstructor
public class SystemNotification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @Column(nullable = false, length = 100)
    private String title;
    
    @Column(nullable = false, length = 500)
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;
    
    /**
     * 특정 사용자 대상일 경우의 사용자 ID 목록 (JSON 배열로 저장)
     */
    @Column(columnDefinition = "TEXT")
    private String targetUserIds;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;
    
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    /**
     * 예약 발송 시간 (즉시 발송이면 null)
     */
    private OffsetDateTime scheduledAt;
    
    /**
     * 실제 발송된 시간
     */
    private OffsetDateTime sentAt;
    
    /**
     * 생성 시점에 KST 시간으로 설정
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        if (this.status == null) {
            this.status = NotificationStatus.PENDING;
        }
    }
    
    /**
     * 알림 유형
     */
    public enum NotificationType {
        SYSTEM_ANNOUNCEMENT("시스템 공지사항"),
        ADMIN_MESSAGE("운영자 메시지"), 
        UPDATE_NOTIFICATION("업데이트 알림"),
        MAINTENANCE_NOTICE("점검 안내"),
        EVENT_NOTIFICATION("이벤트 알림");
        
        private final String displayName;
        
        NotificationType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 발송 대상 유형
     */
    public enum TargetType {
        ALL("전체 사용자"),
        SPECIFIC("특정 사용자");
        
        private final String displayName;
        
        TargetType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 우선순위
     */
    public enum Priority {
        LOW("낮음"),
        NORMAL("보통"),
        HIGH("높음");
        
        private final String displayName;
        
        Priority(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 알림 상태
     */
    public enum NotificationStatus {
        PENDING("대기"),
        SENDING("발송중"),
        SENT("발송완료"),
        FAILED("발송실패"),
        SCHEDULED("예약됨");
        
        private final String displayName;
        
        NotificationStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // 편의 메소드들
    
    /**
     * 즉시 발송인지 확인
     */
    public boolean isImmediateSend() {
        return scheduledAt == null;
    }
    
    /**
     * 예약 발송인지 확인
     */
    public boolean isScheduledSend() {
        return scheduledAt != null;
    }
    
    /**
     * 발송 완료인지 확인
     */
    public boolean isSent() {
        return status == NotificationStatus.SENT;
    }
    
    /**
     * 높은 우선순위인지 확인
     */
    public boolean isHighPriority() {
        return priority == Priority.HIGH;
    }
    
    /**
     * 전체 사용자 대상인지 확인
     */
    public boolean isTargetAll() {
        return targetType == TargetType.ALL;
    }
}