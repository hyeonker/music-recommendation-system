package com.example.musicrecommendation.web;

import com.example.musicrecommendation.domain.ChatReport;
import com.example.musicrecommendation.service.ChatReportService;
import com.example.musicrecommendation.service.UserService;
import com.example.musicrecommendation.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 채팅 신고 관리 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/chat-reports")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000"})
public class ChatReportController {
    
    private final ChatReportService chatReportService;
    private final UserService userService;
    private final ChatMessageRepository chatMessageRepository;
    
    /**
     * 통합 관리자 권한 확인 - OAuth2 + 로컬 로그인 지원
     */
    private boolean isAdminUnified(OAuth2User oauth2User, HttpServletRequest request) {
        log.info("=== 통합 관리자 권한 확인 시작 ===");
        
        // 1. OAuth2 로그인 확인
        if (oauth2User != null) {
            log.info("OAuth2 사용자 감지");
            return isAdmin(oauth2User);
        }
        
        // 2. 로컬 로그인 확인
        HttpSession session = request.getSession(false);
        if (session != null) {
            Long userId = (Long) session.getAttribute("userId");
            String authProvider = (String) session.getAttribute("authProvider");
            
            log.info("로컬 세션 감지 - userId: {}, authProvider: {}", userId, authProvider);
            
            if (userId != null && "LOCAL".equals(authProvider)) {
                boolean isAdmin = userService.isAdmin(userId);
                log.info("로컬 사용자 관리자 권한 확인 결과: {}", isAdmin);
                return isAdmin;
            }
        }
        
        log.warn("인증되지 않은 사용자 또는 권한 없음");
        return false;
    }

    /**
     * 관리자 권한 확인 - OAuth2 전용
     */
    private boolean isAdmin(OAuth2User oauth2User) {
        if (oauth2User == null) return false;
        
        try {
            Long userId = getUserId(oauth2User);
            
            log.info("=== 관리자 권한 확인 시작 ===");
            log.info("사용자 ID: {}", userId);
            
            // 사용자 ID로 확인 (가장 안전)
            if (userId != null && userService.isAdmin(userId)) {
                log.info("관리자 권한 확인됨 - 사용자 ID: {}", userId);
                return true;
            }
            
            log.warn("관리자 권한 없음 - 사용자 ID: {}", userId);
            return false;
        } catch (Exception e) {
            log.error("관리자 권한 확인 중 오류: ", e);
            return false;
        }
    }
    
    private Long getUserId(OAuth2User oauth2User) {
        try {
            Map<String, Object> attrs = oauth2User.getAttributes();
            String extractedEmail = extractEmailFromOAuth2User(attrs);
            
            if (extractedEmail != null) {
                return userService



                        .findUserByEmail(extractedEmail)
                    .map(user -> user.getId())
                    .orElse(null);
            }
            return null;
        } catch (Exception e) {
            log.error("사용자 ID 조회 실패", e);
            return null;
        }
    }
    
    private String extractEmailFromOAuth2User(Map<String, Object> attrs) {
        if (attrs.containsKey("email")) {
            return (String) attrs.get("email");
        }
        return null;
    }
    
    /**
     * 기본 정보 조회 (API 상태 확인용)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getApiInfo() {
        return ResponseEntity.ok(Map.of(
            "service", "Chat Report API",
            "status", "active",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
    
    /**
     * 디버그용 - 데이터베이스의 채팅 메시지 확인
     */
    @GetMapping("/debug/messages")
    public ResponseEntity<Map<String, Object>> debugMessages() {
        try {
            // 최근 채팅 메시지 조회
            List<Object[]> recentMessages = chatMessageRepository.findRecentMessagesForDebug();
            
            List<Map<String, Object>> messageList = recentMessages.stream()
                .map(msg -> Map.of(
                    "id", msg[0],
                    "roomId", msg[1],
                    "senderId", msg[2], 
                    "createdAt", msg[3]
                ))
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "messageCount", messageList.size(),
                "messages", messageList,
                "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.error("디버그 메시지 조회 실패", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }
    
    /**
     * 사용자 - 신고 제출
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitReport(@RequestBody Map<String, Object> request) {
        try {
            Long reporterId = Long.valueOf(request.get("reporterId").toString());
            Long reportedUserId = Long.valueOf(request.get("reportedUserId").toString());
            String chatRoomId = request.get("chatRoomId").toString();
            ChatReport.ReportType reportType = ChatReport.ReportType.valueOf(request.get("reportType").toString());
            String reason = request.get("reason").toString();
            String additionalInfo = (String) request.get("additionalInfo");
            
            ChatReport report = chatReportService.submitReport(
                reporterId, reportedUserId, chatRoomId, reportType, reason, additionalInfo
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "신고가 성공적으로 접수되었습니다",
                "reportId", report.getId(),
                "timestamp", LocalDateTime.now().toString()
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.error("신고 제출 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "신고 제출 중 오류가 발생했습니다",
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }
    
    /**
     * 관리자 - 모든 신고 목록 조회 (페이징)
     */
    @GetMapping("/admin/all")
    public ResponseEntity<Map<String, Object>> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest request) {
        
        if (!isAdminUnified(oauth2User, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "관리자 권한이 필요합니다."
            ));
        }
        try {
            Page<ChatReport> reports = chatReportService.getAllReports(page, size);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "reports", reports.getContent(),
                "currentPage", page,
                "totalPages", reports.getTotalPages(),
                "totalElements", reports.getTotalElements(),
                "timestamp", LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            log.error("신고 목록 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "신고 목록 조회 중 오류가 발생했습니다",
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }
    
    /**
     * 관리자 - 특정 상태의 신고 목록 조회
     */
    @GetMapping("/admin/status/{status}")
    public ResponseEntity<Map<String, Object>> getReportsByStatus(
            @PathVariable ChatReport.ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest request) {
        
        if (!isAdminUnified(oauth2User, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "관리자 권한이 필요합니다."
            ));
        }
        try {
            Page<ChatReport> reports = chatReportService.getReportsByStatus(status, page, size);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "reports", reports.getContent(),
                "status", status,
                "currentPage", page,
                "totalPages", reports.getTotalPages(),
                "totalElements", reports.getTotalElements(),
                "timestamp", LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            log.error("상태별 신고 목록 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "신고 목록 조회 중 오류가 발생했습니다",
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }
    
    /**
     * 신고 상세 조회
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<Map<String, Object>> getReportById(@PathVariable Long reportId) {
        try {
            return chatReportService.getReportById(reportId)
                .map(report -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "report", report,
                    "timestamp", LocalDateTime.now().toString()
                )))
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            log.error("신고 상세 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "신고 조회 중 오류가 발생했습니다",
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }
    
    /**
     * 관리자 - 신고 상태 업데이트
     */
    @PutMapping("/admin/{reportId}/status")
    public ResponseEntity<Map<String, Object>> updateReportStatus(
            @PathVariable Long reportId,
            @RequestBody Map<String, Object> requestBody,
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest request) {
        
        if (!isAdminUnified(oauth2User, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "관리자 권한이 필요합니다."
            ));
        }
        try {
            ChatReport.ReportStatus status = ChatReport.ReportStatus.valueOf(requestBody.get("status").toString());
            String adminResponse = (String) requestBody.get("adminResponse");
            Long adminUserId = Long.valueOf(requestBody.get("adminUserId").toString());
            
            ChatReport updatedReport = chatReportService.updateReportStatus(
                reportId, status, adminResponse, adminUserId
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "신고 상태가 업데이트되었습니다",
                "report", updatedReport,
                "timestamp", LocalDateTime.now().toString()
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.error("신고 상태 업데이트 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "신고 상태 업데이트 중 오류가 발생했습니다",
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }
    
    /**
     * 사용자 - 내 신고 내역 조회
     */
    @GetMapping("/user/{userId}/reports")
    public ResponseEntity<Map<String, Object>> getUserReports(@PathVariable Long userId) {
        try {
            List<ChatReport> reports = chatReportService.getReportsByReporter(userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "reports", reports,
                "totalCount", reports.size(),
                "timestamp", LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            log.error("사용자 신고 내역 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "신고 내역 조회 중 오류가 발생했습니다",
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }
    
    /**
     * 관리자 - 특정 사용자에 대한 신고 내역 조회 (피신고자)
     */
    @GetMapping("/admin/user/{userId}/reported")
    public ResponseEntity<Map<String, Object>> getReportsForUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest request) {
        
        if (!isAdminUnified(oauth2User, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "관리자 권한이 필요합니다."
            ));
        }
        try {
            List<ChatReport> reports = chatReportService.getReportsForUser(userId);
            long activeCount = chatReportService.getActiveReportCount(userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "reports", reports,
                "totalCount", reports.size(),
                "activeCount", activeCount,
                "timestamp", LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            log.error("사용자 피신고 내역 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "피신고 내역 조회 중 오류가 발생했습니다",
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }
    
    /**
     * 관리자 - 신고 통계 정보
     */
    @GetMapping("/admin/statistics")
    public ResponseEntity<Map<String, Object>> getReportStatistics(
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest request) {
        
        if (!isAdminUnified(oauth2User, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "관리자 권한이 필요합니다."
            ));
        }
        try {
            Map<String, Object> stats = chatReportService.getReportStatistics();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "statistics", stats,
                "timestamp", LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            log.error("신고 통계 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "통계 조회 중 오류가 발생했습니다",
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }
    
    /**
     * 관리자 - 신고 관련 채팅 메시지 조회
     */
    @GetMapping("/admin/{reportId}/messages")
    public ResponseEntity<Map<String, Object>> getChatMessages(
            @PathVariable Long reportId,
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest request) {
        
        if (!isAdminUnified(oauth2User, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "관리자 권한이 필요합니다."
            ));
        }
        try {
            ChatReport report = chatReportService.getReportById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다: " + reportId));
            
            // 채팅 메시지 조회 (신고 시점까지의 메시지)
            List<Map<String, Object>> messages = chatReportService.getChatMessagesForReport(
                report.getChatRoomId(), 
                report.getCreatedAt()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "messages", messages,
                "chatRoomId", report.getChatRoomId(),
                "timestamp", LocalDateTime.now().toString()
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.error("채팅 메시지 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "채팅 메시지 조회 중 오류가 발생했습니다",
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }
    
    /**
     * 신고 유형 목록 조회
     */
    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> getReportTypes() {
        try {
            Map<String, String> types = Map.of(
                "INAPPROPRIATE_MESSAGE", "부적절한 메시지",
                "HARASSMENT", "괴롭힘/혐오", 
                "SPAM", "스팸/도배",
                "FAKE_PROFILE", "허위 프로필",
                "SEXUAL_CONTENT", "성적인 내용",
                "VIOLENCE_THREAT", "폭력/위협",
                "OTHER", "기타"
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "reportTypes", types,
                "timestamp", LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            log.error("신고 유형 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "신고 유형 조회 중 오류가 발생했습니다",
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }
}