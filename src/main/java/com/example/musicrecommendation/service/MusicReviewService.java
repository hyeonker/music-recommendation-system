package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.*;
import com.example.musicrecommendation.repository.*;
import com.example.musicrecommendation.domain.ReviewReport;
import com.example.musicrecommendation.security.ReviewSecurityService;
import com.example.musicrecommendation.web.dto.ReviewDto;
import com.example.musicrecommendation.web.dto.BadgeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MusicReviewService {

    private final MusicReviewRepository reviewRepository;
    private final MusicItemRepository musicItemRepository;
    private final UserBadgeRepository badgeRepository;
    private final UserBadgeService badgeService;
    private final ReviewSecurityService securityService;
    private final ReviewHelpfulRepository reviewHelpfulRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final UserService userService;
    private final UserBehaviorTrackingService behaviorTrackingService;
    
    @Transactional
    @CacheEvict(value = {"reviews", "user-names"}, allEntries = true)
    public MusicReview createReview(Long userId, String externalId, MusicItem.MusicItemType itemType,
                                   String musicName, String artistName, String albumName, String imageUrl,
                                   Integer rating, String reviewText, List<String> tags) {
        
        // 보안 검증
        if (!securityService.canCreateReview(userId, externalId)) {
            throw new IllegalStateException("리뷰 생성 제한에 걸렸습니다. 잠시 후 다시 시도해주세요.");
        }
        
        validateRating(rating);
        
        // 리뷰 내용 검증
        ReviewSecurityService.ReviewValidationResult validation = securityService.validateReviewContent(reviewText);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getMessage());
        }
        
        log.info("리뷰 작성 요청 - 사용자: {}, 곡명: '{}', 아티스트: '{}', externalId: '{}'", userId, musicName, artistName, externalId);
        
        MusicItem musicItem = findOrCreateMusicItem(externalId, itemType, musicName, artistName, albumName, imageUrl);
        log.info("MusicItem 생성/조회 완료 - ID: {}, 이름: '{}', 아티스트: '{}'", musicItem.getId(), musicItem.getName(), musicItem.getArtistName());
        
        Optional<MusicReview> existingReview = reviewRepository.findByUserIdAndMusicItem(userId, musicItem);
        if (existingReview.isPresent()) {
            throw new IllegalStateException("이미 이 음악에 대한 리뷰를 작성하셨습니다.");
        }
        
        MusicReview review = MusicReview.builder()
                .userId(userId)
                .musicItem(musicItem)
                .rating(rating)
                .reviewText(reviewText)
                .tags(tags)
                .build();
        
        log.info("📝 리뷰 생성 직전 - MusicItem ID: {}, 곡명: '{}', 아티스트: '{}'", 
                musicItem.getId(), musicItem.getName(), musicItem.getArtistName());
        
        MusicReview savedReview = reviewRepository.save(review);
        
        log.info("💾 리뷰 저장 완료 - Review ID: {}, MusicItem ID: {}, 곡명: '{}'", 
                savedReview.getId(), savedReview.getMusicItem().getId(), savedReview.getMusicItem().getName());
        
        // 배지 시스템 체크
        badgeService.checkAndAwardBadges(userId);
        
        // 리뷰 작성 행동 추적 (비동기)
        behaviorTrackingService.trackReviewEvent(userId, externalId, 
            mapItemType(itemType), rating.doubleValue(), reviewText);
        
        log.info("✅ 새 리뷰 작성 완료 - 사용자: {}, 음악: '{}' (ID: {}), 평점: {}, 리뷰 ID: {}", 
                userId, musicItem.getName(), musicItem.getId(), rating, savedReview.getId());
        log.info("🔄 캐시 무효화 완료 - reviews, user-names");
        
        return savedReview;
    }
    
    @Transactional
    @CacheEvict(value = "reviews", allEntries = true)
    public MusicReview updateReview(Long reviewId, Long userId, Integer rating, String reviewText, List<String> tags) {
        
        // 보안 검증
        if (!securityService.canUpdateReview(reviewId, userId)) {
            throw new IllegalStateException("리뷰를 수정할 권한이 없거나 수정 가능한 시간이 지났습니다.");
        }
        
        MusicReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        
        if (!review.getUserId().equals(userId)) {
            throw new IllegalStateException("자신의 리뷰만 수정할 수 있습니다.");
        }
        
        // 리뷰 내용 검증
        if (reviewText != null) {
            ReviewSecurityService.ReviewValidationResult validation = securityService.validateReviewContent(reviewText);
            if (!validation.isValid()) {
                throw new IllegalArgumentException(validation.getMessage());
            }
        }
        
        if (rating != null) {
            validateRating(rating);
            review.setRating(rating);
        }
        
        if (reviewText != null) {
            review.setReviewText(reviewText);
        }
        
        if (tags != null) {
            review.setTags(tags);
        }
        
        return reviewRepository.save(review);
    }
    
    @Transactional
    @CacheEvict(value = "reviews", allEntries = true)
    public void deleteReview(Long reviewId, Long userId) {
        MusicReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        
        if (!review.getUserId().equals(userId)) {
            throw new IllegalStateException("자신의 리뷰만 삭제할 수 있습니다.");
        }
        
        reviewRepository.delete(review);
        log.info("리뷰 삭제 완료 - ID: {}, 사용자: {}", reviewId, userId);
    }
    
    // @Cacheable(value = "reviews", key = "'item_' + #musicItemId")
    public Page<MusicReview> getReviewsByMusicItem(Long musicItemId, Pageable pageable) {
        MusicItem musicItem = musicItemRepository.findById(musicItemId)
                .orElseThrow(() -> new IllegalArgumentException("음악 아이템을 찾을 수 없습니다."));
        
        return reviewRepository.findByMusicItemAndIsPublicTrue(musicItem, pageable);
    }
    
    // @Cacheable(value = "reviews", key = "'user_' + #userId")
    public Page<MusicReview> getReviewsByUser(Long userId, Pageable pageable) {
        return reviewRepository.findByUserIdAndIsPublicTrue(userId, pageable);
    }
    
    // @Cacheable(value = "reviews", key = "'recent_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<MusicReview> getRecentReviews(Pageable pageable) {
        Page<MusicReview> reviews = reviewRepository.findByIsPublicTrueOrderByCreatedAtDesc(pageable);
        
        // 각 리뷰의 MusicItem을 강제로 다시 로딩 (디버깅용)
        reviews.getContent().forEach(review -> {
            MusicItem musicItem = musicItemRepository.findById(review.getMusicItem().getId()).orElse(null);
            if (musicItem != null) {
                review.setMusicItem(musicItem);
                log.info("🔍 리뷰 ID: {}, MusicItem 재로딩: ID={}, 곡명='{}'", 
                        review.getId(), musicItem.getId(), musicItem.getName());
            }
        });
        
        return reviews;
    }
    
    // @Cacheable(value = "reviews", key = "'helpful_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<MusicReview> getMostHelpfulReviews(Pageable pageable) {
        return reviewRepository.findMostHelpfulReviews(1, pageable);
    }
    
    // @Cacheable(value = "reviews", key = "'rated_' + #minRating + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<MusicReview> getHighRatedReviews(Integer minRating, Pageable pageable) {
        return reviewRepository.findHighRatedReviews(minRating, pageable);
    }
    
    public Page<MusicReview> searchReviews(String tag, Integer minRating, Integer maxRating, Pageable pageable) {
        return reviewRepository.findReviewsWithFilters(tag, minRating, maxRating, pageable);
    }
    
    public Optional<Double> getAverageRating(Long musicItemId) {
        MusicItem musicItem = musicItemRepository.findById(musicItemId)
                .orElseThrow(() -> new IllegalArgumentException("음악 아이템을 찾을 수 없습니다."));
        
        return reviewRepository.findAverageRatingByMusicItem(musicItem);
    }
    
    public long getReviewCount(Long musicItemId) {
        MusicItem musicItem = musicItemRepository.findById(musicItemId)
                .orElseThrow(() -> new IllegalArgumentException("음악 아이템을 찾을 수 없습니다."));
        
        return reviewRepository.countByMusicItemAndIsPublicTrue(musicItem);
    }
    
    public long getUserReviewCount(Long userId) {
        return reviewRepository.countByUserId(userId);
    }
    
    public List<String> getAllTags() {
        return reviewRepository.findAllTags();
    }
    
    @Transactional
    @CacheEvict(value = {"reviews", "user-names"}, allEntries = true)
    public void markReviewAsHelpful(Long reviewId, Long userId) {
        log.info("도움이 됨 처리 시작: reviewId={}, userId={}", reviewId, userId);
        
        // 보안 검증
        if (!securityService.canMarkReviewAsHelpful(reviewId, userId)) {
            log.warn("보안 검증 실패: reviewId={}, userId={}", reviewId, userId);
            throw new IllegalStateException("이 리뷰를 도움이 됨으로 표시할 수 없습니다.");
        }
        
        MusicReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        
        log.info("리뷰 정보: reviewId={}, reviewUserId={}, currentUserId={}", reviewId, review.getUserId(), userId);
        
        if (review.getUserId().equals(userId)) {
            log.warn("자기 리뷰에 도움이 됨 시도: reviewId={}, userId={}", reviewId, userId);
            throw new IllegalStateException("자신의 리뷰는 도움이 됨으로 표시할 수 없습니다.");
        }
        
        // 이미 도움이 됨을 누른 경우 중복 방지
        boolean alreadyMarked = reviewHelpfulRepository.existsByReviewIdAndUserId(reviewId, userId);
        log.info("중복 체크 결과: reviewId={}, userId={}, alreadyMarked={}", reviewId, userId, alreadyMarked);
        
        if (alreadyMarked) {
            log.warn("이미 도움이 됨 표시됨: reviewId={}, userId={}", reviewId, userId);
            throw new IllegalStateException("이미 이 리뷰를 도움이 됨으로 표시했습니다.");
        }
        
        // ReviewHelpful 레코드 생성
        ReviewHelpful helpful = ReviewHelpful.builder()
                .reviewId(reviewId)
                .userId(userId)
                .build();
        reviewHelpfulRepository.save(helpful);
        
        // 카운트 증가
        review.setHelpfulCount(review.getHelpfulCount() + 1);
        reviewRepository.save(review);
        
        // 리뷰어에게 배지 체크
        badgeService.checkAndAwardBadges(review.getUserId());
    }
    
    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1~5 사이의 값이어야 합니다.");
        }
    }
    
    private MusicItem findOrCreateMusicItem(String externalId, MusicItem.MusicItemType itemType,
                                           String musicName, String artistName, String albumName, String imageUrl) {
        log.info("findOrCreateMusicItem 호출 - externalId: '{}', 곡명: '{}', 아티스트: '{}'", externalId, musicName, artistName);
        
        // externalId가 null이거나 빈 문자열인 경우 새 아이템 생성
        if (externalId == null || externalId.trim().isEmpty()) {
            log.warn("externalId가 null 또는 빈 문자열입니다. 새 MusicItem을 생성합니다.");
            MusicItem newItem = MusicItem.builder()
                    .externalId("manual-" + System.currentTimeMillis()) // 고유 ID 생성
                    .itemType(itemType)
                    .name(musicName)
                    .artistName(artistName)
                    .albumName(albumName)
                    .imageUrl(imageUrl)
                    .build();
            MusicItem savedItem = musicItemRepository.save(newItem);
            log.info("새 MusicItem 생성 (externalId 없음) - ID: {}, 이름: '{}', 아티스트: '{}'", savedItem.getId(), savedItem.getName(), savedItem.getArtistName());
            return savedItem;
        }
        
        Optional<MusicItem> existing = musicItemRepository.findByExternalIdAndItemType(externalId, itemType);
        if (existing.isPresent()) {
            MusicItem existingItem = existing.get();
            log.info("기존 MusicItem 발견 - ID: {}, 이름: '{}', 아티스트: '{}', externalId: '{}'", 
                    existingItem.getId(), existingItem.getName(), existingItem.getArtistName(), existingItem.getExternalId());
            
            // 기존 아이템과 요청된 정보가 다른 경우 경고
            if (!musicName.equals(existingItem.getName()) || !artistName.equals(existingItem.getArtistName())) {
                log.warn("⚠️ 기존 MusicItem과 요청 정보가 다릅니다!");
                log.warn("   기존: '{}' by '{}'", existingItem.getName(), existingItem.getArtistName());
                log.warn("   요청: '{}' by '{}'", musicName, artistName);
            }
            
            return existingItem;
        } else {
            MusicItem newItem = MusicItem.builder()
                    .externalId(externalId)
                    .itemType(itemType)
                    .name(musicName)
                    .artistName(artistName)
                    .albumName(albumName)
                    .imageUrl(imageUrl)
                    .build();
            MusicItem savedItem = musicItemRepository.save(newItem);
            log.info("새 MusicItem 생성 - ID: {}, 이름: '{}', 아티스트: '{}', externalId: '{}'", 
                    savedItem.getId(), savedItem.getName(), savedItem.getArtistName(), savedItem.getExternalId());
            return savedItem;
        }
    }
    
    /**
     * MusicReview를 사용자 닉네임이 포함된 ReviewDto.Response로 변환
     */
    // @Cacheable(value = "user-names", key = "#review.userId")
    public ReviewDto.Response convertToResponseWithUserNickname(MusicReview review) {
        String userNickname = userService.findUserById(review.getUserId())
                .map(User::getName)
                .orElse("알 수 없는 사용자");
        
        // 대표 배지 정보도 가져오기
        var representativeBadge = badgeService.getRepresentativeBadge(review.getUserId());
        var representativeBadgeDto = representativeBadge != null 
                ? BadgeDto.Response.from(representativeBadge) 
                : null;
        
        return ReviewDto.Response.fromWithUserInfo(review, userNickname, representativeBadgeDto);
    }
    
    /**
     * 추천 시스템용: 사용자의 높은 평점 리뷰 조회
     */
    public List<MusicReview> getHighRatedReviewsByUser(Long userId, Integer minRating) {
        return reviewRepository.findByUserIdAndRatingGreaterThanEqual(userId, minRating);
    }
    
    /**
     * 추천 시스템용: 사용자 작성 리뷰 + 도움이 됨 표시한 리뷰 조회
     */
    public List<MusicReview> getReviewsForRecommendations(Long userId, Integer minRating) {
        List<MusicReview> reviews = reviewRepository.findReviewsForRecommendations(userId, minRating);
        
        // MusicItem을 강제로 로딩 (native query로 인해 lazy loading 문제 해결)
        reviews.forEach(review -> {
            if (review.getMusicItem() != null) {
                musicItemRepository.findById(review.getMusicItem().getId())
                    .ifPresent(review::setMusicItem);
            }
        });
        
        return reviews;
    }
    
    /**
     * 리뷰 신고하기
     */
    @Transactional
    public ReviewReport reportReview(Long reviewId, Long reporterUserId, ReviewReport.ReportReason reason, String description) {
        log.info("리뷰 신고 처리 시작: reviewId={}, reporterUserId={}, reason={}", reviewId, reporterUserId, reason);
        
        // 리뷰 존재 확인
        MusicReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        
        // 자신의 리뷰는 신고할 수 없음
        if (review.getUserId().equals(reporterUserId)) {
            throw new IllegalStateException("자신의 리뷰는 신고할 수 없습니다.");
        }
        
        // 이미 신고한 경우 중복 방지
        if (reviewReportRepository.existsByReviewIdAndReporterUserId(reviewId, reporterUserId)) {
            throw new IllegalStateException("이미 신고 접수된 내역입니다.");
        }
        
        // 신고 생성
        ReviewReport report = ReviewReport.builder()
                .reviewId(reviewId)
                .reporterUserId(reporterUserId)
                .reason(reason)
                .description(description)
                .build();
        
        ReviewReport savedReport = reviewReportRepository.save(report);
        
        // 신고 건수 증가
        long reportCount = reviewReportRepository.countByReviewId(reviewId);
        review.setReportCount((int) reportCount);
        reviewRepository.save(review);
        
        log.info("리뷰 신고 완료 - 신고 ID: {}, 리뷰 ID: {}, 총 신고 건수: {}", 
                savedReport.getId(), reviewId, reportCount);
        
        return savedReport;
    }
    
    /**
     * 리뷰의 신고 건수 조회
     */
    public long getReportCount(Long reviewId) {
        return reviewReportRepository.countByReviewId(reviewId);
    }
    
    /**
     * 사용자가 특정 리뷰를 신고했는지 확인
     */
    public boolean hasUserReportedReview(Long reviewId, Long userId) {
        return reviewReportRepository.existsByReviewIdAndReporterUserId(reviewId, userId);
    }
    
    /**
     * MusicItem.MusicItemType을 UserBehaviorEvent.ItemType으로 변환
     */
    private UserBehaviorEvent.ItemType mapItemType(MusicItem.MusicItemType musicItemType) {
        return switch (musicItemType) {
            case TRACK -> UserBehaviorEvent.ItemType.SONG;
            case ALBUM -> UserBehaviorEvent.ItemType.ALBUM;
            case ARTIST -> UserBehaviorEvent.ItemType.ARTIST;
            case PLAYLIST -> UserBehaviorEvent.ItemType.PLAYLIST;
        };
    }
    
    /**
     * 리뷰 기반 추천을 위한 유사 평점 사용자 찾기
     */
    public List<Long> findUsersWithSimilarRatingPatterns(Long userId, int limit) {
        try {
            List<MusicReview> userReviews = reviewRepository.findByUserId(userId);
            if (userReviews.isEmpty()) return List.of();
            
            // 사용자의 평균 평점 계산
            double userAvgRating = userReviews.stream()
                .mapToInt(MusicReview::getRating)
                .average()
                .orElse(3.0);
            
            // ±0.5 범위의 유사한 평점 패턴을 가진 사용자들 찾기
            return reviewRepository.findUsersWithSimilarAverageRating(userId, userAvgRating, 0.5, limit);
            
        } catch (Exception e) {
            log.error("유사 평점 패턴 사용자 검색 실패 (userId: {}): {}", userId, e.getMessage());
            return List.of();
        }
    }
    
    /**
     * 특정 아이템에 대한 평점 분포 조회
     */
    public Map<Integer, Long> getRatingDistribution(Long musicItemId) {
        try {
            List<Object[]> distribution = reviewRepository.findRatingDistributionByMusicItemId(musicItemId);
            return distribution.stream()
                .collect(Collectors.toMap(
                    arr -> ((Number) arr[0]).intValue(),
                    arr -> ((Number) arr[1]).longValue()
                ));
        } catch (Exception e) {
            log.error("평점 분포 조회 실패 (musicItemId: {}): {}", musicItemId, e.getMessage());
            return Map.of();
        }
    }
}