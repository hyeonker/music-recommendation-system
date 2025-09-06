package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.ChatReport;
import com.example.musicrecommendation.domain.SystemNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 신고 관련 알림 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportNotificationService {
    
    private final UserService userService;
    private final SystemNotificationService systemNotificationService;
    
    /**
     * 신고자에게 상태 변경 알림 전송
     */
    public void sendReportStatusNotification(ChatReport report, String adminResponse, String adminUserName) {
        try {
            log.info("📢 신고 상태 변경 알림 전송 시작 - 신고 ID: {}, 상태: {}", 
                    report.getId(), report.getStatus());
            
            String statusMessage = getStatusMessage(report.getStatus());
            String notificationMessage = createNotificationMessage(report, statusMessage, adminResponse, adminUserName);
            
            // 실제 알림 전송 (현재는 로그로 시뮬레이션)
            sendNotificationToUser(report.getReporterId(), notificationMessage);
            
            log.info("✅ 신고 상태 변경 알림 전송 완료 - 사용자 ID: {}", report.getReporterId());
            
        } catch (Exception e) {
            log.error("❌ 신고 알림 전송 실패 - 신고 ID: {}, 오류: {}", report.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 상태별 메시지 생성
     */
    private String getStatusMessage(ChatReport.ReportStatus status) {
        return switch (status) {
            case UNDER_REVIEW -> "검토 시작";
            case RESOLVED -> "해결됨";
            case DISMISSED -> "기각됨";
            default -> "상태 변경";
        };
    }
    
    /**
     * 알림 메시지 생성
     */
    private String createNotificationMessage(ChatReport report, String statusMessage, String adminResponse, String adminUserName) {
        String reportTypeKorean = getReportTypeKorean(report.getReportType());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        
        StringBuilder message = new StringBuilder();
        message.append("🚨 신고 상태 업데이트\n\n");
        message.append("📋 신고 ID: ").append(report.getId()).append("\n");
        message.append("📝 신고 유형: ").append(reportTypeKorean).append("\n");
        message.append("📊 상태: ").append(statusMessage).append("\n");
        message.append("👮 담당 관리자: ").append(adminUserName).append("\n");
        message.append("⏰ 처리 시간: ").append(timestamp).append("\n\n");
        
        if (adminResponse != null && !adminResponse.trim().isEmpty()) {
            message.append("💬 관리자 답변:\n");
            message.append(adminResponse).append("\n\n");
        }
        
        // 상태별 추가 안내
        switch (report.getStatus()) {
            case UNDER_REVIEW -> message.append("📋 담당자가 신고 내용을 검토 중입니다. 조속한 시일 내에 결과를 안내드리겠습니다.");
            case RESOLVED -> message.append("✅ 신고가 해결되었습니다. 추가 문의사항이 있으시면 고객센터로 연락해주세요.");
            case DISMISSED -> message.append("❌ 신고 내용을 검토한 결과 기각되었습니다. 추가 정보가 있으시면 새로운 신고를 접수해주세요.");
        }
        
        return message.toString();
    }
    
    /**
     * 신고 유형 한국어 변환
     */
    private String getReportTypeKorean(ChatReport.ReportType reportType) {
        return switch (reportType) {
            case INAPPROPRIATE_MESSAGE -> "부적절한 메시지";
            case HARASSMENT -> "괴롭힘/혐오";
            case SPAM -> "스팸/도배";
            case FAKE_PROFILE -> "허위 프로필";
            case SEXUAL_CONTENT -> "성적인 내용";
            case VIOLENCE_THREAT -> "폭력/위협";
            case OTHER -> "기타";
        };
    }
    
    /**
     * 사용자에게 알림 전송 (기존 SystemNotificationService 활용)
     */
    private void sendNotificationToUser(Long userId, String message) {
        try {
            log.info("🔔 기존 알림 시스템을 통한 사용자 알림 전송 - 사용자 ID: {}", userId);
            
            // SystemNotificationService를 활용한 알림 생성
            SystemNotificationService.SystemNotificationCreateRequest request = 
                new SystemNotificationService.SystemNotificationCreateRequest();
            request.setType(SystemNotification.NotificationType.ADMIN_MESSAGE);
            request.setTitle("🚨 신고 처리 결과 알림");
            request.setMessage(message);
            request.setTargetType(SystemNotification.TargetType.SPECIFIC);
            request.setTargetUserIds(List.of(userId));
            request.setPriority(SystemNotification.Priority.HIGH);
            request.setScheduledAt(null); // 즉시 발송
            
            // 알림 생성 및 발송
            SystemNotificationService.SystemNotificationSendResult result = 
                systemNotificationService.createAndSendNotification(request);
            
            log.info("✅ 신고 처리 알림 전송 완료 - 사용자 ID: {}, 알림 ID: {}", 
                    userId, result.getNotificationId());
            
        } catch (Exception e) {
            log.error("❌ 사용자 알림 전송 실패 - 사용자 ID: {}, 오류: {}", userId, e.getMessage());
            throw e;
        }
    }
}