package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.UserBadge;
import com.example.musicrecommendation.repository.MusicReviewRepository;
import com.example.musicrecommendation.repository.UserBadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserBadgeService {

    private final UserBadgeRepository badgeRepository;
    private final MusicReviewRepository reviewRepository;
    
    @Cacheable(value = "user-badges", key = "#userId")
    public List<UserBadge> getUserBadges(Long userId) {
        return badgeRepository.findByUserIdOrderByEarnedAtDesc(userId);
    }
    
    public long getUserBadgeCount(Long userId) {
        return badgeRepository.countByUserId(userId);
    }
    
    public boolean hasBadge(Long userId, UserBadge.BadgeType badgeType) {
        return badgeRepository.existsByUserIdAndBadgeType(userId, badgeType);
    }
    
    @Transactional
    @CacheEvict(value = "user-badges", key = "#userId")
    public UserBadge awardBadge(Long userId, UserBadge.BadgeType badgeType, String customName, String customDescription) {
        
        if (badgeRepository.existsByUserIdAndBadgeType(userId, badgeType)) {
            log.debug("사용자 {}는 이미 {} 배지를 보유하고 있습니다.", userId, badgeType);
            return badgeRepository.findByUserIdAndBadgeType(userId, badgeType).orElse(null);
        }
        
        UserBadge badge = UserBadge.builder()
                .userId(userId)
                .badgeType(badgeType)
                .badgeName(customName != null ? customName : badgeType.getDefaultName())
                .description(customDescription != null ? customDescription : getDefaultDescription(badgeType))
                .iconUrl(getIconUrl(badgeType))
                .build();
        
        UserBadge savedBadge = badgeRepository.save(badge);
        
        log.info("새 배지 획득! 사용자: {}, 배지: {} ({})", userId, badgeType, savedBadge.getBadgeName());
        
        return savedBadge;
    }
    
    @Transactional
    public void checkAndAwardBadges(Long userId) {
        
        // 첫 리뷰 배지
        if (!hasBadge(userId, UserBadge.BadgeType.FIRST_REVIEW)) {
            long reviewCount = reviewRepository.countByUserId(userId);
            if (reviewCount >= 1) {
                awardBadge(userId, UserBadge.BadgeType.FIRST_REVIEW, null, null);
            }
        }
        
        // 리뷰 마스터 배지 (10개 이상 리뷰)
        if (!hasBadge(userId, UserBadge.BadgeType.REVIEW_MASTER)) {
            long reviewCount = reviewRepository.countByUserId(userId);
            if (reviewCount >= 10) {
                awardBadge(userId, UserBadge.BadgeType.REVIEW_MASTER, null, "10개 이상의 리뷰를 작성했습니다!");
            }
        }
        
        // 음악 평론가 배지 (50개 이상 리뷰)
        if (!hasBadge(userId, UserBadge.BadgeType.CRITIC)) {
            long reviewCount = reviewRepository.countByUserId(userId);
            if (reviewCount >= 50) {
                awardBadge(userId, UserBadge.BadgeType.CRITIC, null, "진정한 음악 평론가! 50개 이상의 리뷰를 작성했습니다!");
            }
        }
        
        // 도움이 되는 리뷰어 배지 (도움이 됨 5개 이상 받은 리뷰 보유)
        // TODO: 도움이 됨 수를 체크하는 로직 구현 필요
    }
    
    public List<Object[]> getBadgeStatistics() {
        return badgeRepository.getBadgeTypeStatistics();
    }
    
    public List<Long> getUsersWithBadge(UserBadge.BadgeType badgeType) {
        return badgeRepository.findUserIdsByBadgeType(badgeType);
    }
    
    private String getDefaultDescription(UserBadge.BadgeType badgeType) {
        return switch (badgeType) {
            case FIRST_REVIEW -> "첫 번째 리뷰를 작성했습니다!";
            case REVIEW_MASTER -> "많은 리뷰를 작성한 숙련된 리뷰어입니다.";
            case HELPFUL_REVIEWER -> "다른 사용자들에게 도움이 되는 리뷰를 작성합니다.";
            case CRITIC -> "전문가 수준의 음악 평론가입니다.";
            case GENRE_EXPLORER -> "다양한 음악 장르를 탐험합니다.";
            case EARLY_ADOPTER -> "새로운 음악을 빠르게 발견합니다.";
            case MUSIC_DISCOVERER -> "숨겨진 음악 보석을 찾아냅니다.";
            case SOCIAL_BUTTERFLY -> "활발한 소셜 활동을 합니다.";
            case FRIEND_MAKER -> "많은 음악 친구를 만들었습니다.";
            case CHAT_MASTER -> "대화의 달인입니다.";
            case BETA_TESTER -> "베타 테스터로 참여해주셔서 감사합니다!";
            case ANNIVERSARY -> "특별한 기념일을 축하합니다.";
            case SPECIAL_EVENT -> "특별한 이벤트에 참여했습니다.";
        };
    }
    
    private String getIconUrl(UserBadge.BadgeType badgeType) {
        // DiceBear API를 사용한 유니크한 배지 아이콘 생성
        String seed = badgeType.name();
        String style = switch (badgeType) {
            // 리뷰 관련 배지 - 귀여운 스타일
            case FIRST_REVIEW -> "bottts";
            case REVIEW_MASTER -> "avataaars";
            case HELPFUL_REVIEWER -> "fun-emoji";
            case CRITIC -> "lorelei";
            
            // 음악 탐험 배지 - 모험적인 스타일
            case GENRE_EXPLORER -> "adventurer";
            case EARLY_ADOPTER -> "bottts";
            case MUSIC_DISCOVERER -> "shapes";
            
            // 소셜 활동 배지 - 친근한 스타일
            case SOCIAL_BUTTERFLY -> "avataaars";
            case FRIEND_MAKER -> "personas";
            case CHAT_MASTER -> "notionists";
            
            // 특별 배지 - 특별한 스타일
            case BETA_TESTER -> "bottts-neutral";
            case ANNIVERSARY -> "fun-emoji";
            case SPECIAL_EVENT -> "thumbs";
        };
        
        // DiceBear API URL 생성 (항상 접근 가능한 공개 API)
        return String.format("https://api.dicebear.com/7.x/%s/svg?seed=%s&backgroundColor=transparent&size=128", 
                style, seed);
    }
}