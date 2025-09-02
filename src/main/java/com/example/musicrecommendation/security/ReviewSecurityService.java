package com.example.musicrecommendation.security;

import com.example.musicrecommendation.domain.MusicReview;
import com.example.musicrecommendation.repository.MusicReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewSecurityService {

    private final MusicReviewRepository reviewRepository;
    
    // 스팸 패턴 정의
    private static final Pattern SPAM_PATTERN = Pattern.compile(
        ".*(광고|홍보|돈|벌기|사기|클릭|링크|www\\.|http|쿠폰|할인|무료|당첨).*",
        Pattern.CASE_INSENSITIVE
    );
    
    // 욕설 패턴 (간단한 예시)
    private static final Pattern PROFANITY_PATTERN = Pattern.compile(
        ".*(바보|멍청|짜증|욕설|비속어).*",
        Pattern.CASE_INSENSITIVE
    );
    
    public boolean canCreateReview(Long userId, String externalId) {
        // 사용자의 최근 리뷰 작성 빈도 체크 (스팸 방지)
        OffsetDateTime oneHourAgo = OffsetDateTime.now(java.time.ZoneId.of("Asia/Seoul")).minusHours(1);
        long recentReviews = reviewRepository.findByUserId(userId)
                .stream()
                .filter(review -> review.getCreatedAt().isAfter(oneHourAgo))
                .count();
        
        if (recentReviews >= 100) { // 임시로 제한을 100개로 늘림
            log.warn("사용자 {}의 시간당 리뷰 생성 제한 초과: {}개", userId, recentReviews);
            return false;
        }
        
        return true;
    }
    
    public boolean canUpdateReview(Long reviewId, Long userId) {
        Optional<MusicReview> reviewOpt = reviewRepository.findById(reviewId);
        
        if (reviewOpt.isEmpty()) {
            return false;
        }
        
        MusicReview review = reviewOpt.get();
        
        // 본인의 리뷰만 수정 가능
        if (!review.getUserId().equals(userId)) {
            log.warn("사용자 {}가 다른 사용자의 리뷰 {} 수정 시도", userId, reviewId);
            return false;
        }
        
        // 생성 후 24시간 이내만 수정 가능
        OffsetDateTime dayAgo = OffsetDateTime.now(java.time.ZoneId.of("Asia/Seoul")).minusHours(24);
        if (review.getCreatedAt().isBefore(dayAgo)) {
            log.warn("리뷰 {} 수정 시간 초과 (24시간)", reviewId);
            return false;
        }
        
        return true;
    }
    
    public boolean canDeleteReview(Long reviewId, Long userId) {
        Optional<MusicReview> reviewOpt = reviewRepository.findById(reviewId);
        
        if (reviewOpt.isEmpty()) {
            return false;
        }
        
        MusicReview review = reviewOpt.get();
        
        // 본인의 리뷰만 삭제 가능
        return review.getUserId().equals(userId);
    }
    
    public ReviewValidationResult validateReviewContent(String reviewText) {
        if (reviewText == null || reviewText.trim().isEmpty()) {
            return new ReviewValidationResult(true, null);
        }
        
        // 길이 체크
        if (reviewText.length() > 2000) {
            return new ReviewValidationResult(false, "리뷰는 2000자를 초과할 수 없습니다.");
        }
        
        // 스팸 패턴 체크
        if (SPAM_PATTERN.matcher(reviewText).matches()) {
            log.warn("스팸 패턴 감지된 리뷰: {}", reviewText.substring(0, Math.min(50, reviewText.length())));
            return new ReviewValidationResult(false, "부적절한 내용이 포함되어 있습니다.");
        }
        
        // 욕설 패턴 체크
        if (PROFANITY_PATTERN.matcher(reviewText).matches()) {
            log.warn("욕설 패턴 감지된 리뷰: {}", reviewText.substring(0, Math.min(50, reviewText.length())));
            return new ReviewValidationResult(false, "부적절한 언어가 포함되어 있습니다.");
        }
        
        // 반복 문자 체크
        if (hasExcessiveRepetition(reviewText)) {
            return new ReviewValidationResult(false, "과도한 반복 문자가 포함되어 있습니다.");
        }
        
        return new ReviewValidationResult(true, null);
    }
    
    public boolean canMarkReviewAsHelpful(Long reviewId, Long userId) {
        Optional<MusicReview> reviewOpt = reviewRepository.findById(reviewId);
        
        if (reviewOpt.isEmpty()) {
            return false;
        }
        
        MusicReview review = reviewOpt.get();
        
        // 자신의 리뷰는 도움이 됨으로 표시할 수 없음
        if (review.getUserId().equals(userId)) {
            return false;
        }
        
        // TODO: 이미 도움이 됨으로 표시했는지 체크하는 로직 추가 필요
        // (review_interactions 테이블 구현 후)
        
        return true;
    }
    
    private boolean hasExcessiveRepetition(String text) {
        // 같은 문자가 5번 이상 반복되는지 체크
        Pattern repetitionPattern = Pattern.compile("(.)\\1{4,}");
        return repetitionPattern.matcher(text).find();
    }
    
    public static class ReviewValidationResult {
        private final boolean valid;
        private final String message;
        
        public ReviewValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
}