package com.example.musicrecommendation.web;

import com.example.musicrecommendation.domain.UserBadge;
import com.example.musicrecommendation.domain.UserRepository;
import com.example.musicrecommendation.service.BadgeNotificationService;
import com.example.musicrecommendation.service.UserBadgeService;
import com.example.musicrecommendation.web.dto.BadgeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
@Slf4j
public class UserBadgeController {

    private final UserBadgeService badgeService;
    private final UserRepository userRepository;
    private final BadgeNotificationService notificationService;
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BadgeDto.Response>> getUserBadges(@PathVariable Long userId) {
        
        var badges = badgeService.getUserBadges(userId)
                .stream()
                .map(BadgeDto.Response::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(badges);
    }
    
    @GetMapping("/my")
    public ResponseEntity<List<BadgeDto.Response>> getMyBadges(
            @AuthenticationPrincipal OAuth2User oauth2User,
            jakarta.servlet.http.HttpServletRequest request) {
        
        Long userId = getUserId(oauth2User, request);
        
        var badges = badgeService.getUserBadges(userId)
                .stream()
                .map(BadgeDto.Response::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(badges);
    }
    
    @GetMapping("/my/no-cache")
    public ResponseEntity<List<BadgeDto.Response>> getMyBadgesNoCache(
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        Long userId = getUserId(oauth2User);
        
        var badges = badgeService.getUserBadgesNoCache(userId)
                .stream()
                .map(BadgeDto.Response::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(badges);
    }
    
    @GetMapping("/user/{userId}/profile")
    public ResponseEntity<BadgeDto.UserBadgeProfile> getUserBadgeProfile(@PathVariable Long userId) {
        
        var badges = badgeService.getUserBadges(userId);
        long totalBadges = badgeService.getUserBadgeCount(userId);
        
        // 희귀도별 개수 계산
        long commonBadges = badges.stream()
                .mapToLong(badge -> "COMMON".equals(badge.getRarity()) ? 1 : 0)
                .sum();
        
        long uncommonBadges = badges.stream()
                .mapToLong(badge -> "UNCOMMON".equals(badge.getRarity()) ? 1 : 0)
                .sum();
        
        long rareBadges = badges.stream()
                .mapToLong(badge -> "RARE".equals(badge.getRarity()) ? 1 : 0)
                .sum();
        
        long epicBadges = badges.stream()
                .mapToLong(badge -> "EPIC".equals(badge.getRarity()) ? 1 : 0)
                .sum();
        
        long legendaryBadges = badges.stream()
                .mapToLong(badge -> "LEGENDARY".equals(badge.getRarity()) ? 1 : 0)
                .sum();
        
        // 최근 배지 (최대 5개)
        List<BadgeDto.Response> recentBadges = badges.stream()
                .limit(5)
                .map(BadgeDto.Response::from)
                .collect(Collectors.toList());
        
        // 특별한 배지 (EPIC, LEGENDARY 등급)
        List<BadgeDto.Response> featuredBadges = badges.stream()
                .filter(badge -> List.of("EPIC", "LEGENDARY").contains(badge.getRarity()))
                .limit(3)
                .map(BadgeDto.Response::from)
                .collect(Collectors.toList());
        
        BadgeDto.UserBadgeProfile profile = BadgeDto.UserBadgeProfile.builder()
                .userId(userId)
                .userNickname("Unknown") // TODO: 사용자 서비스에서 닉네임 가져오기
                .totalBadges(totalBadges)
                .commonBadges(commonBadges)
                .uncommonBadges(uncommonBadges)
                .rareBadges(rareBadges)
                .epicBadges(epicBadges)
                .legendaryBadges(legendaryBadges)
                .recentBadges(recentBadges)
                .featuredBadges(featuredBadges)
                .build();
        
        return ResponseEntity.ok(profile);
    }
    
    @GetMapping("/my/profile")
    public ResponseEntity<BadgeDto.UserBadgeProfile> getMyBadgeProfile(
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        Long userId = getUserId(oauth2User);
        return getUserBadgeProfile(userId);
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<List<BadgeDto.BadgeStatistics>> getBadgeStatistics() {
        
        var statistics = badgeService.getBadgeStatistics()
                .stream()
                .map(stat -> {
                    var badgeType = (com.example.musicrecommendation.domain.UserBadge.BadgeType) stat[0];
                    var count = ((Number) stat[1]).longValue();
                    
                    // 임시로 희귀도 계산 (실제로는 전체 사용자 수 대비 계산해야 함)
                    var rarity = switch (badgeType) {
                        case FIRST_REVIEW, SOCIAL_BUTTERFLY -> "COMMON";
                        case REVIEW_MASTER, GENRE_EXPLORER, FRIEND_MAKER -> "UNCOMMON";
                        case HELPFUL_REVIEWER, CRITIC, EARLY_ADOPTER -> "RARE";
                        case MUSIC_DISCOVERER, CHAT_MASTER -> "EPIC";
                        case BETA_TESTER, ANNIVERSARY, SPECIAL_EVENT, 
                             ADMIN, FOUNDER, COMMUNITY_LEADER, CONTENT_CURATOR, 
                             TRENDSETTER, QUALITY_GUARDIAN, INNOVATION_PIONEER, 
                             MUSIC_SCHOLAR, PLATINUM_MEMBER -> "LEGENDARY";
                    };
                    
                    return BadgeDto.BadgeStatistics.builder()
                            .badgeType(badgeType)
                            .badgeName(badgeType.getDefaultName())
                            .totalEarned(count)
                            .rarity(rarity)
                            .earningRate(count * 100.0 / 1000) // 임시로 1000명 기준
                            .build();
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(statistics);
    }
    
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Long> getUserBadgeCount(@PathVariable Long userId) {
        long count = badgeService.getUserBadgeCount(userId);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/my/count")
    public ResponseEntity<Long> getMyBadgeCount(@AuthenticationPrincipal OAuth2User oauth2User) {
        Long userId = getUserId(oauth2User);
        long count = badgeService.getUserBadgeCount(userId);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/all")
    public ResponseEntity<BadgeDto.AllBadgesResponse> getAllBadgesWithProgress(
            @AuthenticationPrincipal OAuth2User oauth2User,
            jakarta.servlet.http.HttpServletRequest request) {
        
        log.info("🔍 /api/badges/all 호출됨 - OAuth2User: {}", oauth2User != null ? "존재" : "null");
        
        try {
            Long userId = getUserId(oauth2User, request);
            log.info("✅ 사용자 ID 추출 완료: {}", userId);
            
            var allBadgesWithProgress = badgeService.getAllBadgesWithProgress(userId);
            log.info("✅ 전체 배지 데이터 조회 완료 - 배지 개수: {}", allBadgesWithProgress.getBadges().size());
            
            return ResponseEntity.ok(allBadgesWithProgress);
            
        } catch (Exception e) {
            log.warn("❌ 인증되지 않은 사용자가 전체 배지 목록 접근 시도: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }
    
    @PostMapping("/representative")
    public ResponseEntity<String> setRepresentativeBadge(
            @AuthenticationPrincipal OAuth2User oauth2User,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            @RequestBody SetRepresentativeBadgeRequest request) {
        
        Long userId = getUserId(oauth2User, httpRequest);
        
        // 해당 사용자가 실제로 그 배지를 보유하고 있는지 확인
        if (!badgeService.hasBadgeById(userId, request.getBadgeId())) {
            return ResponseEntity.badRequest().body("보유하지 않은 배지는 대표 배지로 설정할 수 없습니다.");
        }
        
        badgeService.setRepresentativeBadge(userId, request.getBadgeId());
        return ResponseEntity.ok("대표 배지가 설정되었습니다.");
    }
    
    @DeleteMapping("/representative")
    public ResponseEntity<String> removeRepresentativeBadge(
            @AuthenticationPrincipal OAuth2User oauth2User,
            jakarta.servlet.http.HttpServletRequest request) {
        
        Long userId = getUserId(oauth2User, request);
        badgeService.setRepresentativeBadge(userId, null);
        return ResponseEntity.ok("대표 배지가 해제되었습니다.");
    }
    
    @GetMapping("/user/{userId}/representative")
    public ResponseEntity<BadgeDto.Response> getRepresentativeBadge(@PathVariable Long userId) {
        
        var representativeBadge = badgeService.getRepresentativeBadge(userId);
        if (representativeBadge == null) {
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.ok(BadgeDto.Response.from(representativeBadge));
    }
    
    // === 관리자 전용 배지 관리 ===
    
    @PostMapping("/admin/award")
    public ResponseEntity<String> awardBadgeToUser(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RequestBody AwardBadgeRequest request) {
        
        Long adminUserId = getUserId(oauth2User);
        
        // 관리자 권한 체크 - 데이터베이스에서 사용자 정보 확인
        var adminUser = userRepository.findById(adminUserId);
        if (adminUser.isEmpty()) {
            log.warn("존재하지 않는 사용자가 배지 부여 시도 - userId: {}", adminUserId);
            return ResponseEntity.status(403).body("관리자 권한이 필요합니다.");
        }
        
        // 관리자 권한 체크 (ID가 1이거나 이름/이메일에 관리자 키워드 포함)
        var user = adminUser.get();
        boolean isAdmin = (user.getId() == 1) || 
                         (user.getName() != null && (user.getName().toLowerCase().contains("admin") || user.getName().contains("운영자"))) ||
                         (user.getEmail() != null && (user.getEmail().toLowerCase().contains("admin") || user.getEmail().equals("root7dll@gmail.com")));
        
        if (!isAdmin) {
            log.warn("관리자가 아닌 사용자가 배지 부여 시도 - userId: {}, name: {}, email: {}", 
                    user.getId(), user.getName(), user.getEmail());
            return ResponseEntity.status(403).body("관리자 권한이 필요합니다.");
        }
        
        log.info("✅ 관리자 권한 확인 완료 - userId: {}, name: {}", user.getId(), user.getName());
        
        try {
            var badge = badgeService.awardBadge(
                request.getUserId(), 
                request.getBadgeType(), 
                request.getCustomName(), 
                request.getCustomDescription(),
                request.getExpiresAt()
            );
            return ResponseEntity.ok("배지가 성공적으로 부여되었습니다: " + badge.getBadgeName());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("배지 부여 실패: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/admin/revoke/{userId}/{badgeType}")
    public ResponseEntity<String> revokeBadgeFromUser(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long userId,
            @PathVariable UserBadge
                    .BadgeType badgeType) {
        
        Long adminUserId = getUserId(oauth2User);
        
        // 관리자 권한 체크 - 데이터베이스에서 사용자 정보 확인
        var adminUser = userRepository.findById(adminUserId);
        if (adminUser.isEmpty()) {
            log.warn("존재하지 않는 사용자가 배지 회수 시도 - userId: {}", adminUserId);
            return ResponseEntity.status(403).body("관리자 권한이 필요합니다.");
        }
        
        // 관리자 권한 체크 (ID가 1이거나 이름/이메일에 관리자 키워드 포함)
        var user = adminUser.get();
        boolean isAdmin = (user.getId() == 1) || 
                         (user.getName() != null && (user.getName().toLowerCase().contains("admin") || user.getName().contains("운영자"))) ||
                         (user.getEmail() != null && (user.getEmail().toLowerCase().contains("admin") || user.getEmail().equals("root7dll@gmail.com")));
        
        if (!isAdmin) {
            log.warn("관리자가 아닌 사용자가 배지 회수 시도 - userId: {}, name: {}, email: {}", 
                    user.getId(), user.getName(), user.getEmail());
            return ResponseEntity.status(403).body("관리자 권한이 필요합니다.");
        }
        
        log.info("✅ 관리자 권한 확인 완료 - userId: {}, name: {}", user.getId(), user.getName());
        
        boolean revoked = badgeService.revokeBadge(userId, badgeType);
        if (revoked) {
            return ResponseEntity.ok("배지가 성공적으로 회수되었습니다.");
        } else {
            return ResponseEntity.badRequest().body("해당 사용자가 이 배지를 보유하고 있지 않습니다.");
        }
    }
    
    // DTO 클래스들
    public static class SetRepresentativeBadgeRequest {
        private Long badgeId;
        
        public Long getBadgeId() { return badgeId; }
        public void setBadgeId(Long badgeId) { this.badgeId = badgeId; }
    }
    
    public static class AwardBadgeRequest {
        private Long userId;
        private UserBadge.BadgeType badgeType;
        private String customName;
        private String customDescription;
        private java.time.OffsetDateTime expiresAt;
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public UserBadge.BadgeType getBadgeType() { return badgeType; }
        public void setBadgeType(UserBadge.BadgeType badgeType) { this.badgeType = badgeType; }
        
        public String getCustomName() { return customName; }
        public void setCustomName(String customName) { this.customName = customName; }
        
        public String getCustomDescription() { return customDescription; }
        public void setCustomDescription(String customDescription) { this.customDescription = customDescription; }
        
        public java.time.OffsetDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(java.time.OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    }

    // OAuth2User와 HttpServletRequest 모두를 지원하는 getUserId 메서드 (LOCAL 사용자 지원)
    private Long getUserId(OAuth2User oauth2User, jakarta.servlet.http.HttpServletRequest request) {
        // OAuth2User가 있으면 OAuth2 방식으로 처리
        if (oauth2User != null) {
            return getUserId(oauth2User);
        }
        
        // OAuth2User가 null이면 LOCAL 세션에서 사용자 ID 추출
        log.info("=== LOCAL 사용자 세션 확인 시작 ===");
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        
        if (session == null) {
            log.warn("❌ 세션이 존재하지 않습니다");
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }
        
        Long userId = (Long) session.getAttribute("userId");
        log.info("세션에서 추출한 userId: {}", userId);
        
        if (userId == null) {
            log.warn("❌ 세션에 userId가 없습니다");
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }
        
        log.info("✅ LOCAL 사용자 인증 성공 - userId: {}", userId);
        return userId;
    }

    private Long getUserId(OAuth2User oauth2User) {
        if (oauth2User == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }
        
        log.info("=== OAuth2User 분석 시작 ===");
        log.info("OAuth2User attributes: {}", oauth2User.getAttributes());
        log.info("OAuth2User name: {}", oauth2User.getName());
        
        // OAuth2User에서 provider와 providerId를 추출하여 데이터베이스에서 사용자 조회
        Map<String, Object> attrs = oauth2User.getAttributes();
        
        // Google OAuth2의 경우
        String providerId = (String) attrs.get("sub");  // Google의 경우 "sub"이 providerId
        String email = (String) attrs.get("email");
        
        // Kakao OAuth2의 경우
        if (providerId == null && attrs.get("id") != null) {
            providerId = String.valueOf(attrs.get("id")); // Kakao의 경우 "id"가 providerId
        }
        
        // Naver OAuth2의 경우
        if (providerId == null) {
            Map<String, Object> response = (Map<String, Object>) attrs.get("response");
            if (response != null) {
                providerId = (String) response.get("id"); // Naver의 경우 response.id가 providerId
                if (email == null) {
                    email = (String) response.get("email");
                }
            }
        }
        
        log.info("추출된 정보 - providerId: {}, email: {}", providerId, email);
        
        // 데이터베이스에서 사용자 찾기
        if (providerId != null) {
            // Google 사용자 먼저 시도
            var googleUser = userRepository.findByProviderAndProviderId(
                com.example.musicrecommendation.domain.AuthProvider.GOOGLE, providerId);
            if (googleUser.isPresent()) {
                log.info("✅ Google 사용자 발견 - ID: {}", googleUser.get().getId());
                return googleUser.get().getId();
            }
            
            // Kakao 사용자 시도
            var kakaoUser = userRepository.findByProviderAndProviderId(
                com.example.musicrecommendation.domain.AuthProvider.KAKAO, providerId);
            if (kakaoUser.isPresent()) {
                log.info("✅ Kakao 사용자 발견 - ID: {}", kakaoUser.get().getId());
                return kakaoUser.get().getId();
            }
            
            // Naver 사용자 시도
            log.info("🔍 Naver 사용자 검색 중 - providerId: {}", providerId);
            var naverUser = userRepository.findByProviderAndProviderId(
                com.example.musicrecommendation.domain.AuthProvider.NAVER, providerId);
            if (naverUser.isPresent()) {
                log.info("✅ Naver 사용자 발견 - ID: {}", naverUser.get().getId());
                return naverUser.get().getId();
            } else {
                log.warn("❌ Naver 사용자 없음 - providerId: {}", providerId);
            }
        }
        
        // 이메일로 사용자 찾기 (providerId가 없는 경우의 대체 방법)
        if (email != null) {
            log.info("🔍 이메일로 사용자 검색 중 - email: {}", email);
            var userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isPresent()) {
                log.info("✅ 이메일로 사용자 발견 - ID: {}", userByEmail.get().getId());
                return userByEmail.get().getId();
            } else {
                log.warn("❌ 이메일로 사용자 없음 - email: {}", email);
            }
        }
        
        log.error("데이터베이스에서 사용자를 찾을 수 없습니다. providerId: {}, email: {}, attributes: {}", 
                 providerId, email, attrs);
        throw new IllegalStateException("데이터베이스에서 사용자를 찾을 수 없습니다. providerId: " + providerId + ", email: " + email);
    }

    @PostMapping("/admin/batch-award")
    public ResponseEntity<Map<String, Object>> batchAwardBadge(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RequestBody BatchAwardBadgeRequest request) {
        
        Long adminUserId = getUserId(oauth2User);
        
        // 관리자 권한 체크
        var adminUser = userRepository.findById(adminUserId);
        if (adminUser.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }
        
        var user = adminUser.get();
        
        // 관리자 권한 확인
        boolean isAdmin = (user.getId() == 1) || 
                         (user.getName() != null && (user.getName().toLowerCase().contains("admin") || user.getName().contains("운영자"))) ||
                         (user.getEmail() != null && (user.getEmail().toLowerCase().contains("admin") || user.getEmail().equals("root7dll@gmail.com")));
        
        if (!isAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }
        
        log.info("✅ 배치 배지 부여 시작 - admin: {}, userIds: {}, badgeType: {}, expiresAt: {}", 
                user.getName(), request.getUserIds(), request.getBadgeType(), request.getExpiresAt());
        
        try {
            var result = badgeService.batchAwardBadge(
                request.getUserIds(), 
                request.getBadgeType(), 
                request.getCustomName(), 
                request.getCustomDescription(),
                request.getExpiresAt()
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "배치 배지 부여가 완료되었습니다.",
                "successCount", result.getSuccessCount(),
                "failCount", result.getFailCount(),
                "results", result.getResults()
            ));
            
        } catch (Exception e) {
            log.error("배치 배지 부여 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", "배치 배지 부여 실패: " + e.getMessage()));
        }
    }

    // 배치 배지 부여 DTO
    public static class BatchAwardBadgeRequest {
        private java.util.List<Long> userIds;
        private UserBadge.BadgeType badgeType;
        private String customName;
        private String customDescription;
        private java.time.OffsetDateTime expiresAt; // 배지 만료일 (선택사항)
        
        public java.util.List<Long> getUserIds() { return userIds; }
        public void setUserIds(java.util.List<Long> userIds) { this.userIds = userIds; }
        
        public UserBadge.BadgeType getBadgeType() { return badgeType; }
        public void setBadgeType(UserBadge.BadgeType badgeType) { this.badgeType = badgeType; }
        
        public String getCustomName() { return customName; }
        public void setCustomName(String customName) { this.customName = customName; }
        
        public String getCustomDescription() { return customDescription; }
        public void setCustomDescription(String customDescription) { this.customDescription = customDescription; }
        
        public java.time.OffsetDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(java.time.OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    }

    // 알림 관련 API (배지, 공지사항, 운영자 메시지 등)
    @GetMapping("/notifications")
    public ResponseEntity<List<BadgeNotificationService.BadgeNotification>> getUserNotifications(
            @RequestParam Long userId) {
        
        var notifications = notificationService.getUserNotifications(userId);
        
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<Long> getUnreadNotificationCount(
            @RequestParam Long userId) {
        
        long unreadCount = notificationService.getUnreadNotificationCount(userId);
        
        return ResponseEntity.ok(unreadCount);
    }
    
    @PostMapping("/notifications/{notificationId}/read")
    public ResponseEntity<String> markNotificationAsRead(
            @PathVariable Long notificationId,
            @RequestBody Map<String, Long> request) {
        
        Long userId = request.get("userId");
        boolean success = notificationService.markAsRead(userId, notificationId);
        
        if (success) {
            return ResponseEntity.ok("알림이 읽음 처리되었습니다.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/notifications/read-all")
    public ResponseEntity<String> markAllNotificationsAsRead(
            @RequestBody Map<String, Long> request) {
        
        Long userId = request.get("userId");
        notificationService.markAllAsRead(userId);
        
        return ResponseEntity.ok("모든 알림이 읽음 처리되었습니다.");
    }
    
    @DeleteMapping("/notifications/{notificationId}")
    public ResponseEntity<String> deleteNotification(
            @PathVariable Long notificationId,
            @RequestBody Map<String, Long> request) {
        
        Long userId = request.get("userId");
        notificationService.deleteNotification(userId, notificationId);
        
        return ResponseEntity.ok("알림이 삭제되었습니다.");
    }
    
    /**
     * 관리자 - 만료 예정 배지 조회 (테스트용)
     */
    @GetMapping("/admin/expiring-badges")
    public ResponseEntity<Map<String, Object>> getExpiringBadges(
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        var user = userRepository.findById(getUserId(oauth2User)).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "사용자를 찾을 수 없습니다."));
        }
        
        boolean isAdmin = isAdminUser(user);
        if (!isAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }
        
        try {
            log.info("🔍 관리자가 만료 예정 배지 조회 시작");
            
            var expiredBadges = badgeService.getExpiredBadges();
            log.info("📊 만료된 배지 {}개 발견", expiredBadges.size());
            
            var expiringBadges = expiredBadges.stream()
                .map(badge -> {
                    var userOpt = userRepository.findById(badge.getUserId());
                    return Map.of(
                        "id", badge.getId(),
                        "userId", badge.getUserId(),
                        "userName", userOpt.map(u -> u.getName()).orElse("Unknown"),
                        "badgeName", badge.getBadgeName(),
                        "badgeType", badge.getBadgeType().toString(),
                        "rarity", getRarityForBadgeType(badge.getBadgeType()),
                        "expiresAt", badge.getExpiresAt() != null ? badge.getExpiresAt().toString() : null
                    );
                })
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "expiringBadges", expiringBadges,
                "count", expiringBadges.size(),
                "currentTime", java.time.OffsetDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.error("만료 예정 배지 조회 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "만료 예정 배지 조회 실패: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 관리자 - 배지 만료 수동 실행 (테스트용)
     */
    @PostMapping("/admin/expire-badges")
    public ResponseEntity<Map<String, Object>> manualExpireBadges(
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        var user = userRepository.findById(getUserId(oauth2User)).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "사용자를 찾을 수 없습니다."));
        }
        
        boolean isAdmin = isAdminUser(user);
        if (!isAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }
        
        log.info("🔧 관리자 수동 배지 만료 실행 - admin: {}", user.getName());
        
        try {
            int removedCount = badgeService.removeExpiredBadges();
            log.info("✅ 수동 배지 만료 완료 - 제거된 배지 수: {}", removedCount);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "removedCount", removedCount,
                "message", removedCount + "개의 만료된 배지가 제거되었습니다."
            ));
        } catch (Exception e) {
            log.error("❌ 수동 배지 만료 실행 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "배지 만료 처리 실패: " + e.getMessage()
            ));
        }
    }
    
    // 관리자 권한 확인 헬퍼 메서드
    private boolean isAdminUser(com.example.musicrecommendation.domain.User user) {
        return (user.getId() == 1) || 
               (user.getName() != null && (user.getName().toLowerCase().contains("admin") || user.getName().contains("운영자"))) ||
               (user.getEmail() != null && (user.getEmail().toLowerCase().contains("admin") || user.getEmail().equals("root7dll@gmail.com")));
    }
    
    /**
     * 관리자 - UTC 배지들을 KST로 변환 (마이그레이션용)
     */
    @PostMapping("/admin/convert-utc-to-kst")
    public ResponseEntity<Map<String, Object>> convertUtcBadgesToKst(
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        var user = userRepository.findById(getUserId(oauth2User)).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "사용자를 찾을 수 없습니다."));
        }
        
        boolean isAdmin = isAdminUser(user);
        if (!isAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }
        
        log.info("🔄 관리자 UTC → KST 배지 변환 시작 - admin: {}", user.getName());
        
        try {
            badgeService.convertUtcBadgesToKst();
            log.info("✅ UTC → KST 배지 변환 완료");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "UTC 배지들이 KST로 변환되었습니다."
            ));
        } catch (Exception e) {
            log.error("❌ UTC → KST 배지 변환 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "UTC → KST 변환 실패: " + e.getMessage()
            ));
        }
    }
    
    // 배지 타입별 희귀도 반환 메서드  
    private String getRarityForBadgeType(UserBadge.BadgeType badgeType) {
        return switch (badgeType) {
            case FIRST_REVIEW, SOCIAL_BUTTERFLY -> "COMMON";
            case REVIEW_MASTER, GENRE_EXPLORER, FRIEND_MAKER -> "UNCOMMON";
            case HELPFUL_REVIEWER, CRITIC, EARLY_ADOPTER -> "RARE";
            case MUSIC_DISCOVERER, CHAT_MASTER -> "EPIC";
            case BETA_TESTER, ANNIVERSARY, SPECIAL_EVENT, 
                 ADMIN, FOUNDER, COMMUNITY_LEADER, CONTENT_CURATOR, 
                 TRENDSETTER, QUALITY_GUARDIAN, INNOVATION_PIONEER, 
                 MUSIC_SCHOLAR, PLATINUM_MEMBER -> "LEGENDARY";
        };
    }
}