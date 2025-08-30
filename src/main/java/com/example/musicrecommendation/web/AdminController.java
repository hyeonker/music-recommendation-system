package com.example.musicrecommendation.web;

import com.example.musicrecommendation.domain.ReviewReport;
import com.example.musicrecommendation.domain.User;
import com.example.musicrecommendation.repository.ReviewReportRepository;
import com.example.musicrecommendation.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    static {
        System.out.println("=== AdminController 클래스 로드됨 ===");
    }

    private final ReviewReportRepository reviewReportRepository;
    private final UserService userService;

    /**
     * 관리자 권한 확인 - 보안 강화 버전
     */
    private boolean isAdmin(OAuth2User oauth2User) {
        if (oauth2User == null) return false;
        
        try {
            Map<String, Object> attrs = oauth2User.getAttributes();
            String extractedEmail = extractEmailFromOAuth2User(attrs);
            Long userId = getUserId(oauth2User);
            
            // 디버깅을 위한 상세 로깅
            log.info("=== 관리자 권한 확인 시작 ===");
            log.info("OAuth2User 속성: {}", attrs);
            log.info("추출된 이메일: {}", extractedEmail);
            log.info("사용자 ID: {}", userId);
            
            // 사용자 이름도 확인해보자
            if (userId != null) {
                Optional<User> userOpt = userService.findUserById(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    log.info("DB에서 조회된 사용자 정보 - ID: {}, 이메일: {}, 이름: {}, Provider: {}", 
                            user.getId(), user.getEmail(), user.getName(), user.getProvider());
                }
            }
            
            // 1차: 사용자 ID로 확인 (가장 안전)
            if (userId != null && userService.isAdmin(userId)) {
                log.info("관리자 권한 확인됨 - 사용자 ID: {}", userId);
                return true;
            }
            
            // 2차: 이메일로 확인 (추가 검증)
            if (extractedEmail != null && !extractedEmail.isBlank()) {
                boolean isAdminByEmail = userService.isAdminByEmail(extractedEmail);
                if (isAdminByEmail) {
                    log.info("관리자 권한 확인됨 - 이메일: {}", extractedEmail);
                    return true;
                }
            }
            
            log.warn("관리자 권한 없음 - 사용자 ID: {}, 이메일: {}", userId, extractedEmail);
            return false;
        } catch (Exception e) {
            log.error("관리자 권한 확인 중 오류: ", e);
            return false;
        }
    }

    /**
     * 모든 신고 내역 조회 (관리자용)
     */
    @GetMapping("/reports")
    public ResponseEntity<?> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        if (!isAdmin(oauth2User)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "관리자 권한이 필요합니다."
            ));
        }

        try {
            Sort.Direction direction = Sort.Direction.fromString(sortDirection);
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<ReviewReport> reports;
            if (status != null && !status.isEmpty()) {
                ReviewReport.ReportStatus reportStatus = ReviewReport.ReportStatus.valueOf(status.toUpperCase());
                reports = reviewReportRepository.findByStatusOrderByCreatedAtDesc(reportStatus, pageable);
            } else {
                reports = reviewReportRepository.findAll(pageable);
            }

            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("신고 내역 조회 중 오류: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "신고 내역을 불러오는 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 처리되지 않은 신고들 조회
     */
    @GetMapping("/reports/pending")
    public ResponseEntity<?> getPendingReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        if (!isAdmin(oauth2User)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "관리자 권한이 필요합니다."
            ));
        }

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ReviewReport> reports = reviewReportRepository.findPendingReports(pageable);
            
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("대기 중인 신고 내역 조회 중 오류: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "대기 중인 신고 내역을 불러오는 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 신고 상태 업데이트
     */
    @PutMapping("/reports/{reportId}/status")
    public ResponseEntity<?> updateReportStatus(
            @PathVariable Long reportId,
            @RequestBody Map<String, Object> updateRequest,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        log.info("=== 상태 업데이트 요청 받음 ===");
        log.info("reportId: {}", reportId);
        log.info("updateRequest: {}", updateRequest);
        log.info("oauth2User가 null인가?: {}", oauth2User == null);
        
        if (!isAdmin(oauth2User)) {
            log.error("관리자 권한 확인 실패");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "관리자 권한이 필요합니다."
            ));
        }

        try {
            Optional<ReviewReport> reportOpt = reviewReportRepository.findById(reportId);
            if (reportOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "신고 내역을 찾을 수 없습니다."
                ));
            }

            ReviewReport report = reportOpt.get();
            String statusStr = (String) updateRequest.get("status");
            ReviewReport.ReportStatus newStatus = ReviewReport.ReportStatus.valueOf(statusStr.toUpperCase());

            report.setStatus(newStatus);
            if (newStatus != ReviewReport.ReportStatus.PENDING) {
                report.setResolvedAt(OffsetDateTime.now());
                
                // 관리자 ID 설정
                Long adminUserId = getUserId(oauth2User);
                if (adminUserId != null) {
                    report.setResolvedByUserId(adminUserId);
                }
            }

            reviewReportRepository.save(report);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "신고 상태가 업데이트되었습니다.",
                "reportId", reportId,
                "status", newStatus.name()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "잘못된 상태값입니다."
            ));
        } catch (Exception e) {
            log.error("신고 상태 업데이트 중 오류: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "신고 상태 업데이트 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 신고 통계 조회
     */
    @GetMapping("/reports/stats")
    public ResponseEntity<?> getReportStats(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (!isAdmin(oauth2User)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "관리자 권한이 필요합니다."
            ));
        }

        try {
            long totalReports = reviewReportRepository.count();
            long pendingReports = reviewReportRepository.findPendingReports(PageRequest.of(0, 1)).getTotalElements();
            long resolvedReports = reviewReportRepository.findByStatusOrderByCreatedAtDesc(
                ReviewReport.ReportStatus.RESOLVED, PageRequest.of(0, 1)).getTotalElements();
            long dismissedReports = reviewReportRepository.findByStatusOrderByCreatedAtDesc(
                ReviewReport.ReportStatus.DISMISSED, PageRequest.of(0, 1)).getTotalElements();

            return ResponseEntity.ok(Map.of(
                "totalReports", totalReports,
                "pendingReports", pendingReports,
                "resolvedReports", resolvedReports,
                "dismissedReports", dismissedReports
            ));
        } catch (Exception e) {
            log.error("신고 통계 조회 중 오류: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "신고 통계를 불러오는 중 오류가 발생했습니다."
            ));
        }
    }

    private Long getUserId(OAuth2User oauth2User) {
        if (oauth2User == null) return null;
        
        try {
            Map<String, Object> attrs = oauth2User.getAttributes();
            String extractedEmail = extractEmailFromOAuth2User(attrs);
            
            if (extractedEmail != null && !extractedEmail.isBlank()) {
                return userService.findUserByEmail(extractedEmail)
                        .map(User::getId)
                        .orElse(null);
            }
            
            return null;
        } catch (Exception e) {
            log.error("사용자 ID 추출 중 오류: ", e);
            return null;
        }
    }

    private String extractEmailFromOAuth2User(Map<String, Object> attrs) {
        // Google 로그인의 경우
        if (attrs.containsKey("sub")) {
            return (String) attrs.get("email");
        }
        // Kakao 로그인의 경우
        else if (attrs.containsKey("id")) {
            Map<String, Object> account = (Map<String, Object>) attrs.get("kakao_account");
            if (account != null) {
                return (String) account.get("email");
            }
        }
        // Naver 로그인의 경우
        else if (attrs.containsKey("response")) {
            Map<String, Object> response = (Map<String, Object>) attrs.get("response");
            if (response != null) {
                return (String) response.get("email");
            }
        }
        
        return null;
    }
}