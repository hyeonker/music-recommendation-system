package com.example.musicrecommendation.web;

import com.example.musicrecommendation.domain.User;
import com.example.musicrecommendation.domain.ReviewReport;
import com.example.musicrecommendation.service.MusicReviewService;
import com.example.musicrecommendation.service.UserService;
import com.example.musicrecommendation.web.dto.ReviewDto;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class MusicReviewController {

    private final MusicReviewService reviewService;
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<ReviewDto.Response> createReview(
            @Valid @RequestBody ReviewDto.CreateRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest servletRequest) {
        
        Long userId = getUserId(oauth2User, servletRequest);
        log.info("리뷰 생성 요청 수신 - 사용자: {}, 곡명: '{}', 아티스트: '{}', externalId: '{}', itemType: {}", 
                userId, request.getMusicName(), request.getArtistName(), request.getExternalId(), request.getItemType());
        
        try {
            var review = reviewService.createReview(
                userId,
                request.getExternalId(),
                request.getItemType(),
                request.getMusicName(),
                request.getArtistName(),
                request.getAlbumName(),
                request.getImageUrl(),
                request.getRating(),
                request.getReviewText(),
                request.getTags()
            );
            
            ReviewDto.Response response = ReviewDto.Response.from(review);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalStateException e) {
            log.warn("리뷰 생성 실패 - 사용자: {}, 이유: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(null);
        } catch (Exception e) {
            log.error("리뷰 생성 중 오류 발생 - 사용자: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
    
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewDto.Response> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewDto.UpdateRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest servletRequest) {
        
        Long userId = getUserId(oauth2User, servletRequest);
        
        log.info("=== 리뷰 수정 요청 ===");
        log.info("reviewId: {}", reviewId);
        log.info("userId: {}", userId);
        log.info("request: rating={}, reviewText={}, tags={}", 
            request.getRating(), request.getReviewText(), request.getTags());
        
        try {
            var review = reviewService.updateReview(
                reviewId,
                userId,
                request.getRating(),
                request.getReviewText(),
                request.getTags()
            );
            
            log.info("리뷰 수정 성공: reviewId={}, userId={}", reviewId, userId);
            
            ReviewDto.Response response = ReviewDto.Response.from(review);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }
    
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest request) {
        
        Long userId = getUserId(oauth2User, request);
        
        try {
            reviewService.deleteReview(reviewId, userId);
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    
    @DeleteMapping("/{reviewId}/admin")
    public ResponseEntity<Map<String, Object>> deleteReviewAsAdmin(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest request) {
        
        try {
            Long userId = getUserId(oauth2User, request);
            
            // 관리자 권한 확인
            if (!userService.isAdmin(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "관리자 권한이 필요합니다."
                ));
            }
            
            // 관리자는 모든 리뷰를 삭제할 수 있음 (작성자 확인 없이)
            reviewService.adminDeleteReview(reviewId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "리뷰가 관리자 권한으로 삭제되었습니다."
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "message", "리뷰를 찾을 수 없습니다."
            ));
        } catch (Exception e) {
            log.error("관리자 리뷰 삭제 중 오류 발생: reviewId={}", reviewId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "리뷰 삭제 중 오류가 발생했습니다."
            ));
        }
    }
    
    @GetMapping("/music-item/{musicItemId}")
    public ResponseEntity<Page<ReviewDto.Response>> getReviewsByMusicItem(
            @PathVariable Long musicItemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<ReviewDto.Response> reviews = reviewService.getReviewsByMusicItem(musicItemId, pageable)
                .map(reviewService::convertToResponseWithUserNickname);
        
        return ResponseEntity.ok(reviews);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ReviewDto.Response>> getReviewsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<ReviewDto.Response> reviews = reviewService.getReviewsByUser(userId, pageable)
                .map(reviewService::convertToResponseWithUserNickname);
        
        return ResponseEntity.ok(reviews);
    }
    
    @GetMapping("/recent")
    public ResponseEntity<Page<ReviewDto.Response>> getRecentReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        Page<ReviewDto.Response> reviews = reviewService.getRecentReviews(pageable)
                .map(reviewService::convertToResponseWithUserNickname);
        
        return ResponseEntity.ok(reviews);
    }
    
    @GetMapping("/helpful")
    public ResponseEntity<Page<ReviewDto.Response>> getMostHelpfulReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        Page<ReviewDto.Response> reviews = reviewService.getMostHelpfulReviews(pageable)
                .map(reviewService::convertToResponseWithUserNickname);
        
        return ResponseEntity.ok(reviews);
    }
    
    @GetMapping("/high-rated")
    public ResponseEntity<Page<ReviewDto.Response>> getHighRatedReviews(
            @RequestParam(defaultValue = "4") Integer minRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        Page<ReviewDto.Response> reviews = reviewService.getHighRatedReviews(minRating, pageable)
                .map(reviewService::convertToResponseWithUserNickname);
        
        return ResponseEntity.ok(reviews);
    }
    
    @PostMapping("/search")
    public ResponseEntity<Page<ReviewDto.Response>> searchReviews(
            @Valid @RequestBody ReviewDto.SearchRequest request) {
        
        Sort.Direction direction = Sort.Direction.fromString(request.getSortDirection());
        Pageable pageable = PageRequest.of(
            request.getPage(), 
            request.getSize(), 
            Sort.by(direction, request.getSortBy())
        );
        
        Page<ReviewDto.Response> reviews = reviewService.searchReviews(
                request.getTag(),
                request.getMinRating(),
                request.getMaxRating(),
                pageable
            ).map(reviewService::convertToResponseWithUserNickname);
        
        return ResponseEntity.ok(reviews);
    }
    
    @PostMapping("/{reviewId}/helpful")
    public ResponseEntity<Void> markAsHelpful(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest servletRequest) {
        
        log.info("도움이 됨 요청 수신: reviewId={}, oauth2User={}, session={}", 
                reviewId, oauth2User != null ? oauth2User.getName() : "null", 
                servletRequest.getSession(false) != null ? "exists" : "null");
        
        try {
            Long userId = getUserId(oauth2User, servletRequest);
            log.info("=== 도움이 됨 처리 시작 ===");
            log.info("OAuth2User: {}, userId: {}, reviewId: {}", oauth2User != null ? "exists" : "null", userId, reviewId);
            
            reviewService.markReviewAsHelpful(reviewId, userId);
            log.info("도움이 됨 처리 성공: reviewId={}, userId={}", reviewId, userId);
            return ResponseEntity.ok().build();
            
        } catch (IllegalArgumentException e) {
            log.warn("도움이 됨 처리 실패 - NOT_FOUND: reviewId={}, error={}", reviewId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            log.warn("도움이 됨 처리 실패 - BAD_REQUEST: reviewId={}, error={}", reviewId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("도움이 됨 처리 중 예상치 못한 오류: reviewId={}", reviewId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/music-item/{musicItemId}/rating")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long musicItemId) {
        Optional<Double> averageRating = reviewService.getAverageRating(musicItemId);
        return averageRating.map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(0.0));
    }
    
    @GetMapping("/music-item/{musicItemId}/count")
    public ResponseEntity<Long> getReviewCount(@PathVariable Long musicItemId) {
        long count = reviewService.getReviewCount(musicItemId);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Long> getUserReviewCount(@PathVariable Long userId) {
        long count = reviewService.getUserReviewCount(userId);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/tags")
    public ResponseEntity<List<String>> getAllTags() {
        List<String> tags = reviewService.getAllTags();
        return ResponseEntity.ok(tags);
    }
    
    @PostMapping("/{reviewId}/report")
    public ResponseEntity<Map<String, Object>> reportReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, Object> reportRequest,
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest servletRequest) {
        
        try {
            Long userId = getUserId(oauth2User, servletRequest);
            String reasonStr = (String) reportRequest.get("reason");
            String description = (String) reportRequest.get("description");
            
            ReviewReport.ReportReason reason = ReviewReport.ReportReason.valueOf(reasonStr);
            
            ReviewReport report = reviewService.reportReview(reviewId, userId, reason, description);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "신고가 접수되었습니다.",
                "reportId", report.getId()
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("리뷰 신고 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "신고 처리 중 오류가 발생했습니다."
            ));
        }
    }
    
    @GetMapping("/{reviewId}/report-status")
    public ResponseEntity<Map<String, Object>> getReportStatus(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest servletRequest) {
        
        try {
            Long userId = getUserId(oauth2User, servletRequest);
            boolean hasReported = reviewService.hasUserReportedReview(reviewId, userId);
            long reportCount = reviewService.getReportCount(reviewId);
            
            return ResponseEntity.ok(Map.of(
                "hasReported", hasReported,
                "reportCount", reportCount
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "hasReported", false,
                "reportCount", 0
            ));
        }
    }
    
    @GetMapping("/admin-status")
    public ResponseEntity<Map<String, Object>> getAdminStatus(
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest servletRequest) {
        
        try {
            Long userId = getUserId(oauth2User, servletRequest);
            boolean isAdmin = userService.isAdmin(userId);
            
            return ResponseEntity.ok(Map.of(
                "isAdmin", isAdmin,
                "userId", userId
            ));
            
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "isAdmin", false,
                "userId", null
            ));
        }
    }
    
    private Long getUserId(OAuth2User oauth2User, HttpServletRequest request) {
        // 1. 먼저 OAuth2 로그인 사용자 확인
        if (oauth2User != null) {
            log.info("OAuth2 사용자 처리: {}", oauth2User.getName());
            Map<String, Object> attrs = oauth2User.getAttributes();
            String extractedEmail = extractEmailFromOAuth2User(attrs);
            
            if (extractedEmail != null && !extractedEmail.isBlank()) {
                return userService.findUserByEmail(extractedEmail)
                        .map(User::getId)
                        .orElseThrow(() -> new IllegalArgumentException("OAuth2 사용자를 찾을 수 없습니다: " + extractedEmail));
            }
        }
        
        // 2. 로컬 세션 로그인 사용자 확인
        HttpSession session = request.getSession(false);
        log.info("세션 정보: session={}, sessionId={}", 
                 session != null ? "exists" : "null", 
                 session != null ? session.getId() : "null");
        
        if (session != null) {
            Long localUserId = (Long) session.getAttribute("userId");
            String authProvider = (String) session.getAttribute("authProvider");
            log.info("세션 속성: userId={}, authProvider={}", localUserId, authProvider);
            
            if (localUserId != null) {
                log.info("로컬 세션 사용자 처리: userId={}", localUserId);
                return localUserId;
            }
        }
        
        log.warn("인증된 사용자 정보를 찾을 수 없습니다. OAuth2User: {}, Session: {}", 
                oauth2User != null ? "exists" : "null",
                session != null ? "exists" : "null");
        throw new IllegalArgumentException("인증된 사용자 정보를 찾을 수 없습니다.");
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