package com.example.musicrecommendation.web;

import com.example.musicrecommendation.domain.SystemNotification;
import com.example.musicrecommendation.domain.User;
import com.example.musicrecommendation.service.SystemNotificationService;
import com.example.musicrecommendation.service.UserService;
import com.example.musicrecommendation.web.dto.SystemNotificationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 시스템 알림 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/notifications/system")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "System Notifications", description = "시스템 알림 관리 API")
public class SystemNotificationController {
    
    private final SystemNotificationService systemNotificationService;
    private final UserService userService;
    
    /**
     * 시스템 알림 목록 조회
     */
    @GetMapping("/list")
    @Operation(summary = "시스템 알림 목록 조회", description = "관리자만 접근 가능")
    public ResponseEntity<List<SystemNotificationDto.Response>> getSystemNotifications(
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        
        // 관리자 권한 확인
        if (!isAdmin(oAuth2User)) {
            log.warn("⚠️ 시스템 알림 조회 권한 없음 - 사용자: {}", getUserId(oAuth2User));
            return ResponseEntity.status(403).build();
        }
        
        try {
            List<SystemNotification> notifications = systemNotificationService.getRecentNotifications();
            List<SystemNotificationDto.Response> response = notifications.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            log.info("📋 시스템 알림 목록 조회 완료 - 관리자: {}, 알림 수: {}", getUserId(oAuth2User), response.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ 시스템 알림 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 시스템 알림 발송
     */
    @PostMapping("/send")
    @Operation(summary = "시스템 알림 발송", description = "관리자만 접근 가능")
    public ResponseEntity<SystemNotificationService.SystemNotificationSendResult> sendSystemNotification(
            @RequestBody SystemNotificationDto.CreateRequest request,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        
        log.info("=== 시스템 알림 발송 요청 받음 ===");
        log.info("요청 내용: {}", request);
        log.info("OAuth2User 정보: {}", oAuth2User != null ? oAuth2User.getAttributes() : "null");
        
        // 관리자 권한 확인
        if (!isAdmin(oAuth2User)) {
            log.warn("⚠️ 시스템 알림 발송 권한 없음 - 사용자: {}", getUserId(oAuth2User));
            return ResponseEntity.status(403).build();
        }
        
        try {
            log.info("🔔 시스템 알림 발송 요청 - 관리자: {}, 유형: {}, 제목: '{}'", 
                getUserId(oAuth2User), request.getType(), request.getTitle());
            
            // 요청 DTO를 서비스 요청 DTO로 변환
            SystemNotificationService.SystemNotificationCreateRequest serviceRequest = 
                convertToServiceRequest(request);
            
            // 알림 생성 및 발송
            SystemNotificationService.SystemNotificationSendResult result = 
                systemNotificationService.createAndSendNotification(serviceRequest);
            
            log.info("✅ 시스템 알림 발송 완료 - 관리자: {}, 알림 ID: {}, 성공: {}명, 실패: {}명", 
                getUserId(oAuth2User), result.getNotificationId(), result.getSuccessCount(), result.getFailCount());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 시스템 알림 발송 실패 - 관리자: {}", getUserId(oAuth2User), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 유형별 알림 통계 조회
     */
    @GetMapping("/stats")
    @Operation(summary = "알림 유형별 통계 조회", description = "관리자만 접근 가능")
    public ResponseEntity<SystemNotificationDto.StatsResponse> getNotificationStats(
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        
        // 관리자 권한 확인
        if (!isAdmin(oAuth2User)) {
            log.warn("⚠️ 시스템 알림 통계 조회 권한 없음 - 사용자: {}", getUserId(oAuth2User));
            return ResponseEntity.status(403).build();
        }
        
        try {
            SystemNotificationDto.StatsResponse stats = new SystemNotificationDto.StatsResponse();
            
            // 각 유형별 개수 조회
            stats.setSystemAnnouncementCount(
                systemNotificationService.getNotificationCountByType(SystemNotification.NotificationType.SYSTEM_ANNOUNCEMENT));
            stats.setAdminMessageCount(
                systemNotificationService.getNotificationCountByType(SystemNotification.NotificationType.ADMIN_MESSAGE));
            stats.setUpdateNotificationCount(
                systemNotificationService.getNotificationCountByType(SystemNotification.NotificationType.UPDATE_NOTIFICATION));
            stats.setMaintenanceNoticeCount(
                systemNotificationService.getNotificationCountByType(SystemNotification.NotificationType.MAINTENANCE_NOTICE));
            stats.setEventNotificationCount(
                systemNotificationService.getNotificationCountByType(SystemNotification.NotificationType.EVENT_NOTIFICATION));
            
            log.info("📊 시스템 알림 통계 조회 완료 - 관리자: {}", getUserId(oAuth2User));
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("❌ 시스템 알림 통계 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Helper methods
    
    /**
     * 관리자 권한 확인 - AdminController와 동일한 로직
     */
    private boolean isAdmin(OAuth2User oauth2User) {
        if (oauth2User == null) return false;
        
        try {
            Map<String, Object> attrs = oauth2User.getAttributes();
            String extractedEmail = extractEmailFromOAuth2User(attrs);
            Long userId = getUserId(oauth2User);
            
            // 디버깅을 위한 상세 로깅
            log.info("=== 시스템 알림 관리자 권한 확인 시작 ===");
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
    
    private SystemNotificationDto.Response convertToDto(SystemNotification notification) {
        SystemNotificationDto.Response dto = new SystemNotificationDto.Response();
        dto.setId(notification.getId());
        dto.setType(notification.getType().name());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setTargetType(notification.getTargetType().name());
        dto.setPriority(notification.getPriority().name());
        dto.setStatus(notification.getStatus().name());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setScheduledAt(notification.getScheduledAt());
        dto.setSentAt(notification.getSentAt());
        
        // 대상 사용자 수 계산
        if (notification.getTargetType() == SystemNotification.TargetType.ALL) {
            dto.setTargetUserCount(null); // 전체 사용자는 null로 표시
        } else {
            // JSON 파싱해서 개수 계산
            String targetUserIds = notification.getTargetUserIds();
            log.info("🔍 대상 사용자 ID JSON: '{}'", targetUserIds);
            
            if (targetUserIds != null && !targetUserIds.trim().isEmpty()) {
                try {
                    // JSON 배열 파싱하여 사용자 수 계산
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.List<?> userIdList = mapper.readValue(targetUserIds, java.util.List.class);
                    dto.setTargetUserCount(userIdList.size());
                    log.info("✅ JSON 파싱 성공 - 대상 사용자 수: {}", userIdList.size());
                } catch (Exception e) {
                    log.warn("❌ 대상 사용자 ID JSON 파싱 실패: '{}', 에러: {}", targetUserIds, e.getMessage());
                    dto.setTargetUserCount(0);
                }
            } else {
                log.warn("⚠️ 대상 사용자 ID가 비어있음");
                dto.setTargetUserCount(0);
            }
        }
        
        return dto;
    }
    
    private SystemNotificationService.SystemNotificationCreateRequest convertToServiceRequest(
            SystemNotificationDto.CreateRequest dto) {
        
        SystemNotificationService.SystemNotificationCreateRequest request = 
            new SystemNotificationService.SystemNotificationCreateRequest();
        
        request.setType(SystemNotification.NotificationType.valueOf(dto.getType()));
        request.setTitle(dto.getTitle());
        request.setMessage(dto.getMessage());
        request.setTargetType(SystemNotification.TargetType.valueOf(dto.getTargetType()));
        request.setTargetUserIds(dto.getTargetUserIds());
        request.setPriority(SystemNotification.Priority.valueOf(dto.getPriority()));
        request.setScheduledAt(dto.getScheduledAt());
        
        return request;
    }
}