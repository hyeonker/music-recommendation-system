package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.ChatReport;
import com.example.musicrecommendation.repository.ChatReportRepository;
import com.example.musicrecommendation.repository.ChatMessageRepository;
import com.example.musicrecommendation.security.AESGcmTextEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 채팅 신고 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatReportService {
    
    private final ChatReportRepository chatReportRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AESGcmTextEncryptor aesGcmTextEncryptor;
    private final ReportNotificationService reportNotificationService;
    private final UserService userService;
    
    /**
     * 신고 제출
     */
    @Transactional
    public ChatReport submitReport(Long reporterId, Long reportedUserId, String chatRoomId,
                                   ChatReport.ReportType reportType, String reason, String additionalInfo) {
        // 중복 신고 방지 (24시간 내)
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        if (chatReportRepository.existsRecentReport(reporterId, reportedUserId, chatRoomId, since)) {
            throw new IllegalArgumentException("이미 24시간 내에 해당 사용자를 신고했습니다.");
        }
        
        // 자기 자신 신고 방지
        if (reporterId.equals(reportedUserId)) {
            throw new IllegalArgumentException("자기 자신을 신고할 수 없습니다.");
        }
        
        ChatReport report = ChatReport.builder()
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .chatRoomId(chatRoomId)
                .reportType(reportType)
                .reason(reason)
                .additionalInfo(additionalInfo)
                .status(ChatReport.ReportStatus.PENDING)
                .build();
        
        ChatReport savedReport = chatReportRepository.save(report);
        
        log.info("새로운 신고 접수: ID={}, 신고자={}, 피신고자={}, 유형={}, 채팅방={}", 
                savedReport.getId(), reporterId, reportedUserId, reportType, chatRoomId);
        
        return savedReport;
    }
    
    /**
     * 관리자 - 모든 신고 목록 조회 (페이징)
     */
    public Page<ChatReport> getAllReports(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chatReportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    /**
     * 관리자 - 특정 상태의 신고 목록 조회 (페이징)
     */
    public Page<ChatReport> getReportsByStatus(ChatReport.ReportStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chatReportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }
    
    /**
     * 신고 상세 조회
     */
    public Optional<ChatReport> getReportById(Long reportId) {
        return chatReportRepository.findById(reportId);
    }
    
    /**
     * 관리자 - 신고 상태 업데이트 (관리자 응답 필수, 알림 전송)
     */
    @Transactional
    public ChatReport updateReportStatus(Long reportId, ChatReport.ReportStatus status, 
                                        String adminResponse, Long adminUserId) {
        ChatReport report = chatReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다: " + reportId));
        
        // 🔒 관리자 응답 필수 조건 검사
        if (status == ChatReport.ReportStatus.UNDER_REVIEW || 
            status == ChatReport.ReportStatus.RESOLVED || 
            status == ChatReport.ReportStatus.DISMISSED) {
            
            if (adminResponse == null || adminResponse.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "검토 시작, 해결, 기각 처리 시에는 관리자 응답 메시지가 필수입니다.");
            }
        }
        
        ChatReport.ReportStatus oldStatus = report.getStatus();
        report.setStatus(status);
        report.setAdminResponse(adminResponse);
        report.setAdminUserId(adminUserId);
        report.setUpdatedAt(LocalDateTime.now());
        
        if (status == ChatReport.ReportStatus.RESOLVED || status == ChatReport.ReportStatus.DISMISSED) {
            report.setResolvedAt(LocalDateTime.now());
        }
        
        ChatReport updatedReport = chatReportRepository.save(report);
        
        log.info("신고 상태 업데이트: ID={}, {} -> {}, 관리자={}", 
                reportId, oldStatus, status, adminUserId);
        
        // 📢 신고자에게 알림 전송
        try {
            String adminUserName = userService.findUserById(adminUserId)
                .map(user -> user.getName())
                .orElse("관리자#" + adminUserId);
            
            reportNotificationService.sendReportStatusNotification(
                updatedReport, adminResponse, adminUserName);
            
            log.info("✅ 신고 상태 변경 알림 전송 완료 - 신고 ID: {}, 신고자: {}", 
                    reportId, updatedReport.getReporterId());
                    
        } catch (Exception e) {
            log.error("⚠️ 신고 알림 전송 실패했지만 상태 업데이트는 성공 - 신고 ID: {}, 오류: {}", 
                    reportId, e.getMessage(), e);
            // 알림 전송 실패해도 상태 업데이트는 성공으로 처리
        }
        
        return updatedReport;
    }
    
    /**
     * 사용자별 신고 내역 조회
     */
    public List<ChatReport> getReportsByReporter(Long reporterId) {
        return chatReportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId);
    }
    
    /**
     * 특정 사용자에 대한 신고 내역 조회 (피신고자)
     */
    public List<ChatReport> getReportsForUser(Long reportedUserId) {
        return chatReportRepository.findByReportedUserIdOrderByCreatedAtDesc(reportedUserId);
    }
    
    /**
     * 특정 채팅방의 신고 내역 조회
     */
    public List<ChatReport> getReportsByChatRoom(String chatRoomId) {
        return chatReportRepository.findByChatRoomIdOrderByCreatedAtDesc(chatRoomId);
    }
    
    /**
     * 관리자 대시보드용 통계 정보
     */
    public Map<String, Object> getReportStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 상태별 신고 개수
        stats.put("pendingCount", chatReportRepository.countByStatus(ChatReport.ReportStatus.PENDING));
        stats.put("underReviewCount", chatReportRepository.countByStatus(ChatReport.ReportStatus.UNDER_REVIEW));
        stats.put("resolvedCount", chatReportRepository.countByStatus(ChatReport.ReportStatus.RESOLVED));
        stats.put("dismissedCount", chatReportRepository.countByStatus(ChatReport.ReportStatus.DISMISSED));
        
        // 총 신고 수
        stats.put("totalCount", chatReportRepository.count());
        
        // 최근 7일간 신고 수
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        stats.put("lastWeekCount", chatReportRepository.countReportsInLastWeek(weekAgo));
        
        // 신고 유형별 통계 (최근 7일)
        List<Object[]> typeStats = chatReportRepository.countByReportTypeInLastWeek(weekAgo);
        Map<String, Long> typeStatsMap = new HashMap<>();
        for (Object[] stat : typeStats) {
            typeStatsMap.put(stat[0].toString(), (Long) stat[1]);
        }
        stats.put("reportTypeStats", typeStatsMap);
        
        return stats;
    }
    
    /**
     * 특정 사용자의 활성 신고 개수 조회
     */
    public long getActiveReportCount(Long userId) {
        return chatReportRepository.countActiveReportsForUser(userId);
    }
    
    /**
     * 관리자별 처리한 신고 개수 조회
     */
    public long getResolvedReportCountByAdmin(Long adminUserId) {
        return chatReportRepository.countResolvedByAdmin(adminUserId);
    }
    
    /**
     * 신고와 관련된 채팅 메시지 조회 (신고 시점까지)
     */
    public List<Map<String, Object>> getChatMessagesForReport(String chatRoomId, LocalDateTime reportTime) {
        try {
            log.info("=== 채팅 메시지 조회 시작 ===");
            log.info("chatRoomId: {}", chatRoomId);
            log.info("신고 시점: {}", reportTime);
            
            // 직접 chatRoomId로 조회 (이제 DB도 같은 형식 사용)
            List<Object[]> messages = chatMessageRepository.findMessagesByRoomIdBeforeTime(
                chatRoomId, reportTime
            );
            
            log.info("조회된 메시지 개수: {}", messages.size());
            
            return messages.stream()
                .map(msg -> {
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("id", msg[0]);
                    
                    // 메시지 복호화 시도 (수사기관 목적)
                    String decryptedContent;
                    try {
                        String contentCipher = (String) msg[1];
                        String ivBase64 = (String) msg[2];
                        decryptedContent = aesGcmTextEncryptor.decrypt(contentCipher, ivBase64);
                        log.debug("메시지 복호화 성공: ID={}", msg[0]);
                    } catch (Exception e) {
                        log.warn("메시지 복호화 실패: ID={}, 오류={}", msg[0], e.getMessage());
                        decryptedContent = "[복호화 실패 - ID: " + msg[0] + "]";
                    }
                    
                    messageMap.put("content", decryptedContent);
                    messageMap.put("senderId", msg[3]);
                    messageMap.put("senderName", "사용자 " + msg[3]);
                    messageMap.put("createdAt", msg[4]);
                    messageMap.put("type", "TEXT");
                    return messageMap;
                })
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("채팅 메시지 조회 실패: chatRoomId={}, reportTime={}", chatRoomId, reportTime, e);
            return new ArrayList<>();
        }
    }
    

    /**
     * 신고 삭제 (관리자 전용, 신중하게 사용)
     */
    @Transactional
    public void deleteReport(Long reportId, Long adminUserId) {
        ChatReport report = chatReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다: " + reportId));
        
        chatReportRepository.delete(report);
        
        log.warn("신고 삭제됨: ID={}, 관리자={}", reportId, adminUserId);
    }
}