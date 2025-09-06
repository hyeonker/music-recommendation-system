package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 채팅 신고 엔티티
 * 사용자가 채팅방에서 다른 사용자를 신고할 때 사용
 */
@Entity
@Table(name = "chat_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;
    
    @Column(name = "reported_user_id", nullable = false) 
    private Long reportedUserId;
    
    @Column(name = "chat_room_id", nullable = false)
    private String chatRoomId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 50)
    private ReportType reportType;
    
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;
    
    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;
    
    @Column(name = "admin_user_id")
    private Long adminUserId;
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    public enum ReportType {
        INAPPROPRIATE_MESSAGE("부적절한 메시지"),
        HARASSMENT("괴롭힘/혐오"),
        SPAM("스팸/도배"),
        FAKE_PROFILE("허위 프로필"),
        SEXUAL_CONTENT("성적인 내용"),
        VIOLENCE_THREAT("폭력/위협"),
        OTHER("기타");
        
        private final String displayName;
        
        ReportType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum ReportStatus {
        PENDING("대기중"),
        UNDER_REVIEW("검토중"), 
        RESOLVED("해결됨"),
        DISMISSED("기각됨");
        
        private final String displayName;
        
        ReportStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.status == ReportStatus.RESOLVED || this.status == ReportStatus.DISMISSED) {
            if (this.resolvedAt == null) {
                this.resolvedAt = LocalDateTime.now();
            }
        }
    }
}