package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.UserBadge;
import com.example.musicrecommendation.event.BadgeEvent;
import com.example.musicrecommendation.repository.MusicReviewRepository;
import com.example.musicrecommendation.repository.UserBadgeRepository;
import com.example.musicrecommendation.domain.UserRepository;
import com.example.musicrecommendation.web.dto.BadgeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.example.musicrecommendation.web.dto.BadgeDto;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserBadgeService {

    private final UserBadgeRepository badgeRepository;
    private final MusicReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final BadgeNotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    
    @Cacheable(value = "user-badges", key = "#userId")
    public List<UserBadge> getUserBadges(Long userId) {
        List<UserBadge> badges = badgeRepository.findByUserIdOrderByEarnedAtDesc(userId);
        log.info("🔍 사용자 {}의 배지 조회 - {}개 발견 (캐시에서 조회)", userId, badges.size());
        
        for (UserBadge badge : badges) {
            log.info("📅 배지: id={}, name={}, type={}, expiresAt={}, 만료여부={}", 
                badge.getId(), badge.getBadgeName(), badge.getBadgeType(), 
                badge.getExpiresAt(), badge.isExpired());
        }
        
        return badges;
    }
    
    // 캐시를 우회하여 사용자 배지 조회 (디버깅용)
    public List<UserBadge> getUserBadgesNoCache(Long userId) {
        List<UserBadge> badges = badgeRepository.findByUserIdOrderByEarnedAtDesc(userId);
        log.info("🔍 사용자 {}의 배지 조회 - {}개 발견 (캐시 우회)", userId, badges.size());
        
        for (UserBadge badge : badges) {
            log.info("📅 배지: id={}, name={}, type={}, expiresAt={}, 만료여부={}", 
                badge.getId(), badge.getBadgeName(), badge.getBadgeType(), 
                badge.getExpiresAt(), badge.isExpired());
        }
        
        return badges;
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
        return awardBadge(userId, badgeType, customName, customDescription, null);
    }
    
    @Transactional
    @CacheEvict(value = "user-badges", key = "#userId")
    public UserBadge awardBadge(Long userId, UserBadge.BadgeType badgeType, String customName, String customDescription, java.time.OffsetDateTime expiresAt) {
        
        if (badgeRepository.existsByUserIdAndBadgeType(userId, badgeType)) {
            log.debug("사용자 {}는 이미 {} 배지를 보유하고 있습니다.", userId, badgeType);
            return badgeRepository.findByUserIdAndBadgeType(userId, badgeType).orElse(null);
        }
        
        log.info("🎖️ 배지 생성 시작 - userId: {}, badgeType: {}, expiresAt: {}", userId, badgeType, expiresAt);
        
        // expiresAt이 있을 경우 KST로 변환해서 저장
        java.time.OffsetDateTime expiresAtKst = null;
        if (expiresAt != null) {
            // 입력받은 시간이 UTC라면 KST로 변환, 이미 KST라면 그대로 사용
            if (expiresAt.getOffset().equals(java.time.ZoneOffset.UTC)) {
                expiresAtKst = expiresAt.atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul")).toOffsetDateTime();
                log.info("🕰️ 만료일 UTC -> KST 변환: {} -> {}", expiresAt, expiresAtKst);
            } else {
                expiresAtKst = expiresAt;
                log.info("🕰️ 만료일 그대로 사용: {}", expiresAtKst);
            }
        }

        UserBadge badge = UserBadge.builder()
                .userId(userId)
                .badgeType(badgeType)
                .badgeName(customName != null ? customName : badgeType.getDefaultName())
                .description(customDescription != null ? customDescription : getDefaultDescription(badgeType))
                .iconUrl(getIconUrl(badgeType))
                .expiresAt(expiresAtKst)
                .build();
        
        log.info("🎖️ 배지 객체 생성 완료 - expiresAt: {}", badge.getExpiresAt());
        
        UserBadge savedBadge = badgeRepository.save(badge);
        
        log.info("🎉 새 배지 획득! 사용자: {}, 배지: {} ({}), 만료일: {}, 저장된 만료일: {}", 
                userId, badgeType, savedBadge.getBadgeName(), expiresAt, savedBadge.getExpiresAt());
        
        // 배지 획득 알림 전송
        try {
            notificationService.sendBadgeAwardedNotification(userId, savedBadge);
        } catch (Exception e) {
            log.warn("배지 획득 알림 전송 실패 - 사용자: {}, 배지: {}", userId, savedBadge.getBadgeName(), e);
        }
        
        // 배지 생성 이벤트 발행 (트랜잭션 완료 후 캐시 무효화됨)
        eventPublisher.publishEvent(BadgeEvent.created(userId, savedBadge.getBadgeName()));
        
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
        
        // 리뷰 마스터 배지 (50개 이상 리뷰)
        if (!hasBadge(userId, UserBadge.BadgeType.REVIEW_MASTER)) {
            long reviewCount = reviewRepository.countByUserId(userId);
            if (reviewCount >= 50) {
                awardBadge(userId, UserBadge.BadgeType.REVIEW_MASTER, null, "50개 이상의 리뷰를 작성한 숙련된 리뷰어!");
            }
        }
        
        // 음악 평론가 배지 (100개 이상 리뷰)
        if (!hasBadge(userId, UserBadge.BadgeType.CRITIC)) {
            long reviewCount = reviewRepository.countByUserId(userId);
            if (reviewCount >= 100) {
                awardBadge(userId, UserBadge.BadgeType.CRITIC, null, "진정한 음악 평론가! 100개 이상의 전문적인 리뷰를 작성했습니다!");
            }
        }
        
        // 도움이 되는 리뷰어 배지 (25번 이상 도움됨 받기)
        if (!hasBadge(userId, UserBadge.BadgeType.HELPFUL_REVIEWER)) {
            // TODO: 실제 도움됨 카운트 로직 구현 필요
            // 현재는 임시로 리뷰 수 기준으로 판정
            long reviewCount = reviewRepository.countByUserId(userId);
            if (reviewCount >= 25) {
                awardBadge(userId, UserBadge.BadgeType.HELPFUL_REVIEWER, null, "다른 사용자들에게 도움이 되는 리뷰를 작성하는 멤버!");
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
    
    // 배지 ID로 사용자가 보유하고 있는지 확인
    public boolean hasBadgeById(Long userId, Long badgeId) {
        return badgeRepository.existsByUserIdAndId(userId, badgeId);
    }
    
    // 대표 배지 설정
    @Transactional
    public void setRepresentativeBadge(Long userId, Long badgeId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        user.setRepresentativeBadgeId(badgeId);
        userRepository.save(user);
        
        log.info("사용자 {}의 대표 배지가 {}로 설정되었습니다.", userId, badgeId);
    }
    
    // 대표 배지 조회
    public UserBadge getRepresentativeBadge(Long userId) {
        var user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRepresentativeBadgeId() == null) {
            return null;
        }
        
        return badgeRepository.findById(user.getRepresentativeBadgeId()).orElse(null);
    }
    
    // 관리자용 배지 회수
    @Transactional
    @CacheEvict(value = "user-badges", key = "#userId")
    public boolean revokeBadge(Long userId, UserBadge.BadgeType badgeType) {
        var badge = badgeRepository.findByUserIdAndBadgeType(userId, badgeType).orElse(null);
        if (badge == null) {
            return false;
        }
        
        // 대표 배지로 설정되어 있다면 해제
        var user = userRepository.findById(userId).orElse(null);
        if (user != null && badge.getId().equals(user.getRepresentativeBadgeId())) {
            user.setRepresentativeBadgeId(null);
            userRepository.save(user);
        }
        
        badgeRepository.delete(badge);
        log.info("배지 회수 완료 - 사용자: {}, 배지: {}", userId, badgeType);
        return true;
    }
    
    /**
     * 모든 배지 유형과 해당 사용자의 진행도를 포함한 전체 배지 정보를 반환
     * @param userId 사용자 ID
     * @return 모든 배지의 진행도 정보
     */
    public BadgeDto.AllBadgesResponse getAllBadgesWithProgress(Long userId) {
        // 현재 사용자가 보유한 배지들을 Map으로 변환
        List<UserBadge> userBadges = badgeRepository.findByUserIdOrderByEarnedAtDesc(userId);
        Map<UserBadge.BadgeType, UserBadge> earnedBadgesMap = userBadges.stream()
                .collect(Collectors.toMap(UserBadge::getBadgeType, badge -> badge));
        
        // 사용자의 현재 상태 정보 조회
        long reviewCount = reviewRepository.countByUserId(userId);
        // TODO: 향후 추가될 메트릭들
        // long helpfulVotes = getHelpfulVotesCount(userId);
        // long genreCount = getExploredGenresCount(userId);
        // long friendCount = getFriendCount(userId);
        // long chatCount = getChatCount(userId);
        
        // 모든 배지 유형에 대해 진행도 정보 생성
        List<BadgeDto.BadgeWithProgress> badgeProgresses = Arrays.stream(UserBadge.BadgeType.values())
                .map(badgeType -> {
                    UserBadge earnedBadge = earnedBadgesMap.get(badgeType);
                    boolean isEarned = earnedBadge != null;
                    
                    BadgeDto.BadgeWithProgress.BadgeWithProgressBuilder builder = BadgeDto.BadgeWithProgress.builder()
                            .badgeType(badgeType)
                            .badgeName(badgeType.getDefaultName())
                            .description(getDefaultDescription(badgeType))
                            .iconUrl(getIconUrl(badgeType))
                            .rarity(getRarityForBadgeType(badgeType))
                            .badgeColor(getBadgeColorForBadgeType(badgeType))
                            .isEarned(isEarned);
                    
                    // 획득한 배지인 경우 획득 정보 설정
                    if (isEarned) {
                        builder.earnedAt(earnedBadge.getEarnedAt())
                               .badgeId(earnedBadge.getId());
                    }
                    
                    // 배지 유형별 진행도 계산
                    ProgressInfo progressInfo = calculateProgress(badgeType, reviewCount, userId);
                    builder.currentProgress(progressInfo.current)
                           .targetProgress(progressInfo.target)
                           .progressText(progressInfo.text);
                    
                    return builder.build();
                })
                .collect(Collectors.toList());
        
        // 전체 통계 계산
        long totalBadges = Arrays.stream(UserBadge.BadgeType.values()).count();
        long earnedBadgesCount = earnedBadgesMap.size();
        double completionRate = totalBadges > 0 ? (double) earnedBadgesCount / totalBadges * 100.0 : 0.0;
        
        return BadgeDto.AllBadgesResponse.builder()
                .badges(badgeProgresses)
                .totalBadges(totalBadges)
                .earnedBadges(earnedBadgesCount)
                .completionRate(Math.round(completionRate * 100.0) / 100.0) // 소수점 둘째 자리까지
                .build();
    }
    
    /**
     * 배지 유형별 진행도 계산
     */
    private ProgressInfo calculateProgress(UserBadge.BadgeType badgeType, long reviewCount, Long userId) {
        return switch (badgeType) {
            case FIRST_REVIEW -> new ProgressInfo(
                    (int) Math.min(reviewCount, 1),
                    1,
                    reviewCount >= 1 ? "첫 리뷰 작성 완료!" : "첫 리뷰를 작성해보세요"
            );
            case REVIEW_MASTER -> new ProgressInfo(
                    (int) Math.min(reviewCount, 50),
                    50,
                    reviewCount >= 50 ? "리뷰 마스터 달성!" : String.format("리뷰 %d/50개 작성", reviewCount)
            );
            case CRITIC -> new ProgressInfo(
                    (int) Math.min(reviewCount, 100),
                    100,
                    reviewCount >= 100 ? "음악 평론가 달성!" : String.format("리뷰 %d/100개 작성", reviewCount)
            );
            case HELPFUL_REVIEWER -> new ProgressInfo(
                    // TODO: 실제 도움이 됨 투표 수로 대체 필요
                    (int) Math.min(reviewCount, 100),
                    100,
                    reviewCount >= 25 ? "도움이 되는 리뷰어 달성!" : String.format("도움이 됨 %d/100개 필요", Math.min(reviewCount, 100))
            );
            // 소셜 활동 배지들 - 현재는 기본 로직으로 처리
            case SOCIAL_BUTTERFLY -> new ProgressInfo(0, 1, "소셜 활동 참여 필요");
            case FRIEND_MAKER -> new ProgressInfo(0, 1, "친구 만들기 기능 준비중");
            case CHAT_MASTER -> new ProgressInfo(0, 1, "채팅 기능 준비중");
            
            // 음악 탐험 배지들
            case GENRE_EXPLORER -> new ProgressInfo(0, 1, "다양한 장르 탐험 필요");
            case MUSIC_DISCOVERER -> new ProgressInfo(0, 1, "새로운 음악 발굴 필요");
            case EARLY_ADOPTER -> new ProgressInfo(0, 1, "새로운 기능 사용 필요");
            
            // 특별 배지들
            case BETA_TESTER -> new ProgressInfo(0, 1, "베타 테스트 참여 필요");
            case ANNIVERSARY -> new ProgressInfo(0, 1, "기념일 이벤트 대기");
            case SPECIAL_EVENT -> new ProgressInfo(0, 1, "특별 이벤트 참여 필요");
            
            // 관리자 전용 배지들
            case ADMIN, FOUNDER, COMMUNITY_LEADER, CONTENT_CURATOR, 
                 TRENDSETTER, QUALITY_GUARDIAN, INNOVATION_PIONEER, 
                 MUSIC_SCHOLAR, PLATINUM_MEMBER -> new ProgressInfo(0, 1, "관리자 권한 필요");
        };
    }
    
    /**
     * 배지 유형별 희귀도 반환
     */
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
    
    /**
     * 배지 유형별 색상 반환
     */
    private String getBadgeColorForBadgeType(UserBadge.BadgeType badgeType) {
        String rarity = getRarityForBadgeType(badgeType);
        return switch (rarity) {
            case "COMMON" -> "#95A5A6";      // 회색
            case "UNCOMMON" -> "#2ECC71";    // 초록
            case "RARE" -> "#3498DB";        // 파랑
            case "EPIC" -> "#9B59B6";        // 보라
            case "LEGENDARY" -> "#F1C40F";   // 금색
            default -> "#BDC3C7";
        };
    }
    
    /**
     * 진행도 정보를 담는 내부 클래스
     */
    private static class ProgressInfo {
        final int current;
        final int target;
        final String text;
        
        ProgressInfo(int current, int target, String text) {
            this.current = current;
            this.target = target;
            this.text = text;
        }
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
            // 운영자 전용 배지
            case ADMIN -> "시스템 운영자입니다!";
            case FOUNDER -> "서비스의 창립자입니다!";
            case COMMUNITY_LEADER -> "커뮤니티를 이끌어가는 리더입니다!";
            case CONTENT_CURATOR -> "훌륭한 콘텐츠를 큐레이션합니다!";
            case TRENDSETTER -> "음악 트렌드를 선도합니다!";
            case QUALITY_GUARDIAN -> "서비스 품질을 관리합니다!";
            case INNOVATION_PIONEER -> "새로운 기능을 개척합니다!";
            case MUSIC_SCHOLAR -> "음악에 대한 깊은 지식을 가지고 있습니다!";
            case PLATINUM_MEMBER -> "최고 등급 멤버입니다!";
        };
    }
    
    private String getIconUrl(UserBadge.BadgeType badgeType) {
        // 각 배지별 특색있는 아이콘 URL 생성
        String seed = badgeType.name().toLowerCase();
        
        // 배지별 맞춤형 색상과 스타일 설정
        String style = switch (badgeType) {
            // 리뷰 관련 배지 - 전문적이고 신뢰성 있는 스타일
            case FIRST_REVIEW -> "bottts"; // 로봇 스타일 (첫 시작의 의미)
            case REVIEW_MASTER -> "adventurer"; // 모험가 스타일 (마스터의 위엄)
            case HELPFUL_REVIEWER -> "fun-emoji"; // 이모지 스타일 (도움의 따뜻함)
            case CRITIC -> "lorelei"; // 우아한 스타일 (비평가의 품격)
            
            // 음악 탐험 배지 - 모험적이고 탐험적인 스타일
            case GENRE_EXPLORER -> "adventurer-neutral"; // 중성적 모험가 (탐험의 중립성)
            case EARLY_ADOPTER -> "shapes"; // 기하학적 (혁신의 추상성)
            case MUSIC_DISCOVERER -> "identicon"; // 유니크 패턴 (발굴의 독특함)
            
            // 소셜 활동 배지 - 친근하고 사교적인 스타일
            case SOCIAL_BUTTERFLY -> "avataaars"; // 아바타 스타일 (소셜의 활발함)
            case FRIEND_MAKER -> "personas"; // 페르소나 스타일 (인맥의 다양성)
            case CHAT_MASTER -> "notionists"; // 노션 스타일 (대화의 현대적 감각)
            
            // 특별 배지 - 축제적이고 특별한 스타일
            case BETA_TESTER -> "bottts-neutral"; // 중성 로봇 (테스터의 객관성)
            case ANNIVERSARY -> "thumbs"; // 썸즈 스타일 (축하의 의미)
            case SPECIAL_EVENT -> "fun-emoji"; // 재미있는 이모지 (이벤트의 즐거움)
            
            // 운영자 전용 배지 - 권위있고 프리미엄한 스타일
            case ADMIN -> "lorelei"; // 우아한 여성 스타일 (관리자의 품격)
            case FOUNDER -> "personas"; // 페르소나 스타일 (창립자의 리더십)
            case COMMUNITY_LEADER -> "adventurer"; // 모험가 스타일 (리더의 개척 정신)
            case CONTENT_CURATOR -> "shapes"; // 기하학적 스타일 (큐레이션의 예술성)
            case TRENDSETTER -> "avataaars"; // 아바타 스타일 (트렌드의 개성)
            case QUALITY_GUARDIAN -> "bottts"; // 로봇 스타일 (품질 관리의 정확성)
            case INNOVATION_PIONEER -> "identicon"; // 아이덴티콘 (혁신의 독창성)
            case MUSIC_SCHOLAR -> "lorelei"; // 우아한 스타일 (학자의 지성)
            case PLATINUM_MEMBER -> "fun-emoji"; // 프리미엄 이모지 (최고 등급의 화려함)
        };
        
        // 배지별 맞춤 색상 설정
        String backgroundColor = switch (badgeType) {
            case FIRST_REVIEW -> "c084fc"; // 보라색 (첫 경험의 신비로움)
            case REVIEW_MASTER -> "3b82f6"; // 파란색 (마스터의 신뢰성)
            case HELPFUL_REVIEWER -> "10b981"; // 초록색 (도움의 긍정성)
            case CRITIC -> "f59e0b"; // 주황색 (비평의 열정)
            
            case GENRE_EXPLORER -> "8b5cf6"; // 보라색 (탐험의 신비)
            case EARLY_ADOPTER -> "06b6d4"; // 하늘색 (얼리어답터의 혁신성)
            case MUSIC_DISCOVERER -> "d946ef"; // 핑크색 (발굴의 특별함)
            
            case SOCIAL_BUTTERFLY -> "f97316"; // 주황색 (소셜의 활발함)
            case FRIEND_MAKER -> "84cc16"; // 라임 (친구의 밝음)
            case CHAT_MASTER -> "6366f1"; // 인디고 (대화의 깊이)
            
            case BETA_TESTER -> "64748b"; // 회색 (테스터의 중립성)
            case ANNIVERSARY -> "dc2626"; // 빨간색 (기념일의 특별함)
            case SPECIAL_EVENT -> "7c3aed"; // 보라색 (이벤트의 신비로움)
            
            // 운영자 배지는 골드/프리미엄 색상
            case ADMIN -> "eab308"; // 골드
            case FOUNDER -> "dc2626"; // 빨간색 (창립의 열정)
            case COMMUNITY_LEADER -> "059669"; // 에메랄드 (리더의 안정감)
            case CONTENT_CURATOR -> "7c2d12"; // 갈색 (큐레이션의 클래식함)
            case TRENDSETTER -> "be123c"; // 로즈 (트렌드의 세련됨)
            case QUALITY_GUARDIAN -> "1e40af"; // 진한 파랑 (품질의 신뢰성)
            case INNOVATION_PIONEER -> "9333ea"; // 보라색 (혁신의 창의성)
            case MUSIC_SCHOLAR -> "166534"; // 진한 초록 (학문의 깊이)
            case PLATINUM_MEMBER -> "4338ca"; // 진한 인디고 (플래티넘의 고급스러움)
        };
        
        // DiceBear API URL 생성 - 더 다양한 옵션으로 커스터마이징
        return String.format("https://api.dicebear.com/7.x/%s/svg?seed=%s&backgroundColor=%s&size=96&radius=20", 
                style, seed, backgroundColor);
    }

    // 배치 배지 부여
    @Transactional
    public BatchAwardResult batchAwardBadge(
            List<Long> userIds, 
            UserBadge.BadgeType badgeType, 
            String customName, 
            String customDescription,
            java.time.OffsetDateTime expiresAt) {
        
        log.info("🎯 배치 배지 부여 시작 - 대상자: {}명, 배지: {}", userIds.size(), badgeType);
        
        List<BatchAwardResult.UserResult> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        
        for (Long userId : userIds) {
            try {
                // 사용자 존재 확인
                if (!userRepository.existsById(userId)) {
                    results.add(new BatchAwardResult.UserResult(userId, false, "사용자를 찾을 수 없습니다."));
                    failCount++;
                    continue;
                }
                
                // 이미 배지를 보유하고 있는지 확인
                if (badgeRepository.existsByUserIdAndBadgeType(userId, badgeType)) {
                    results.add(new BatchAwardResult.UserResult(userId, false, "이미 해당 배지를 보유하고 있습니다."));
                    failCount++;
                    continue;
                }
                
                // 배지 부여
                UserBadge badge = awardBadge(userId, badgeType, customName, customDescription, expiresAt);
                if (badge != null) {
                    results.add(new BatchAwardResult.UserResult(userId, true, "배지 부여 성공"));
                    successCount++;
                } else {
                    results.add(new BatchAwardResult.UserResult(userId, false, "배지 부여 실패"));
                    failCount++;
                }
                
            } catch (Exception e) {
                log.error("사용자 {}에 대한 배지 부여 실패", userId, e);
                results.add(new BatchAwardResult.UserResult(userId, false, "오류: " + e.getMessage()));
                failCount++;
            }
        }
        
        log.info("🎉 배치 배지 부여 완료 - 성공: {}명, 실패: {}명", successCount, failCount);
        
        return new BatchAwardResult(successCount, failCount, results);
    }

    // 만료 예정 배지 조회 (관리자용 - 만료된 것 + 곧 만료될 것)
    public List<UserBadge> getExpiredBadges() {
        java.time.OffsetDateTime nowKst = java.time.OffsetDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        java.time.OffsetDateTime in24Hours = nowKst.plusDays(1); // 24시간 후까지 포함
        
        log.info("🔍 만료 예정 배지 조회 시작");
        log.info("📅 현재 시각 (KST): {}", nowKst);
        log.info("📅 24시간 후 (KST): {}", in24Hours);
        
        // 이미 만료된 배지 + 24시간 내 만료 예정 배지 (KST 기준)
        List<UserBadge> expiredBadges = badgeRepository.findByExpiresAtBefore(nowKst);
        List<UserBadge> soonExpiring = badgeRepository.findByExpiresAtBetween(nowKst, in24Hours);
        
        log.info("🔍 이미 만료된 배지: {}개", expiredBadges.size());
        log.info("🔍 24시간 내 만료 예정 배지: {}개", soonExpiring.size());
        
        // 두 리스트 합치기
        java.util.List<UserBadge> allExpiringBadges = new java.util.ArrayList<>(expiredBadges);
        allExpiringBadges.addAll(soonExpiring);
        
        log.info("🔍 총 만료 관련 배지: {}개", allExpiringBadges.size());
        
        // 모든 만료일 설정된 배지들도 조회해보기 (이제 KST로 저장되어 있음)
        List<UserBadge> allBadgesWithExpiry = badgeRepository.findByExpiresAtIsNotNull();
        log.info("📋 만료일이 설정된 모든 배지 {}개:", allBadgesWithExpiry.size());
        for (UserBadge badge : allBadgesWithExpiry) {
            boolean isExpired = nowKst.isAfter(badge.getExpiresAt()); // 둘 다 KST이므로 직접 비교
            long minutesUntilExpiry = java.time.Duration.between(nowKst, badge.getExpiresAt()).toMinutes();
            log.info("📅 배지: userId={}, badgeName={}, expiresAt={}, 만료여부={}, {}분 후 만료", 
                badge.getUserId(), badge.getBadgeName(), badge.getExpiresAt(), isExpired, minutesUntilExpiry);
        }
        
        return allExpiringBadges;
    }
    
    // 만료된 배지 정리
    @Transactional
    public int removeExpiredBadges() {
        java.time.OffsetDateTime nowKst = java.time.OffsetDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        
        log.info("🧹 만료된 배지 정리 시작");
        log.info("📅 현재 시각 (KST): {}", nowKst);
        
        var expiredBadges = badgeRepository.findByExpiresAtBefore(nowKst);
        log.info("🧹 정리 대상 만료 배지: {}개", expiredBadges.size());
        
        for (UserBadge badge : expiredBadges) {
            log.info("🗑️ 배지 제거 중 - userId: {}, badgeName: {}, expiresAt: {}", 
                badge.getUserId(), badge.getBadgeName(), badge.getExpiresAt());
                
            // 대표 배지로 설정되어 있다면 해제
            var user = userRepository.findById(badge.getUserId()).orElse(null);
            if (user != null && badge.getId().equals(user.getRepresentativeBadgeId())) {
                user.setRepresentativeBadgeId(null);
                userRepository.save(user);
                log.info("📌 대표 배지 해제됨 - userId: {}", badge.getUserId());
            }
            
            // 만료 알림 전송
            try {
                notificationService.sendBadgeExpiredNotification(badge.getUserId(), badge);
                log.info("🔔 만료 알림 전송 완료 - userId: {}", badge.getUserId());
            } catch (Exception e) {
                log.warn("배지 만료 알림 전송 실패 - 사용자: {}, 배지: {}", badge.getUserId(), badge.getBadgeName(), e);
            }
            
            badgeRepository.delete(badge);
            log.info("✅ 만료된 배지 제거 완료 - 사용자: {}, 배지: {}", badge.getUserId(), badge.getBadgeType());
            
            // 배지 만료 이벤트 발행 (트랜잭션 완료 후 캐시 무효화됨)
            eventPublisher.publishEvent(BadgeEvent.expired(badge.getUserId(), badge.getBadgeName()));
        }
        
        log.info("🎉 만료된 배지 정리 완료 - {}개 제거", expiredBadges.size());
        return expiredBadges.size();
    }
    
    // 사용자 배지 캐시 무효화
    @org.springframework.cache.annotation.CacheEvict(value = "user-badges", key = "#userId")
    public void evictUserBadgeCache(Long userId) {
        log.info("🧹 사용자 {}의 배지 캐시 무효화", userId);
    }
    
    // 기존 UTC 배지들을 KST로 변환하는 메서드 (한번만 실행)
    @Transactional
    public void convertUtcBadgesToKst() {
        List<UserBadge> allBadgesWithExpiry = badgeRepository.findByExpiresAtIsNotNull();
        log.info("🔄 UTC -> KST 변환 시작 - 대상 배지: {}개", allBadgesWithExpiry.size());
        
        for (UserBadge badge : allBadgesWithExpiry) {
            OffsetDateTime expiresAt = badge.getExpiresAt();
            
            // UTC 오프셋인지 확인 (Z 또는 +00:00)
            if (expiresAt.getOffset().equals(java.time.ZoneOffset.UTC)) {
                OffsetDateTime kstTime = expiresAt.atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul")).toOffsetDateTime();
                badge.setExpiresAt(kstTime);
                badgeRepository.save(badge);
                
                log.info("🔄 배지 시간대 변환 - userId: {}, badgeName: {}, UTC: {} -> KST: {}", 
                    badge.getUserId(), badge.getBadgeName(), expiresAt, kstTime);
            } else {
                log.info("✅ 이미 KST - userId: {}, badgeName: {}, expiresAt: {}", 
                    badge.getUserId(), badge.getBadgeName(), expiresAt);
            }
        }
        
        log.info("🎉 UTC -> KST 변환 완료!");
    }

    // 만료 예정 배지 알림 전송
    @Transactional(readOnly = true)
    public void sendExpirationNotifications() {
        java.time.OffsetDateTime nowKst = java.time.OffsetDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        java.time.OffsetDateTime threeDaysLater = nowKst.plusDays(3);
        java.time.OffsetDateTime oneDayLater = nowKst.plusDays(1);
        
        // 3일 후 만료 예정 배지들
        var badgesExpiringIn3Days = badgeRepository.findByExpiresAtBetween(
            nowKst, threeDaysLater
        );
        
        for (UserBadge badge : badgesExpiringIn3Days) {
            if (badge.isExpired()) continue; // 이미 만료된 배지는 스킵
            
            long daysUntilExpiry = badge.getDaysUntilExpiry();
            if (daysUntilExpiry <= 3 && daysUntilExpiry > 0) {
                try {
                    notificationService.sendBadgeExpirationNotification(
                        badge.getUserId(), badge, daysUntilExpiry
                    );
                } catch (Exception e) {
                    log.warn("배지 만료 예정 알림 전송 실패 - 사용자: {}, 배지: {}", 
                        badge.getUserId(), badge.getBadgeName(), e);
                }
            }
        }
        
        log.info("배지 만료 예정 알림 전송 완료 - {}개 배지 처리", badgesExpiringIn3Days.size());
    }

    // 배치 부여 결과를 담는 클래스
    public static class BatchAwardResult {
        private final int successCount;
        private final int failCount;
        private final List<UserResult> results;
        
        public BatchAwardResult(int successCount, int failCount, List<UserResult> results) {
            this.successCount = successCount;
            this.failCount = failCount;
            this.results = results;
        }
        
        public int getSuccessCount() { return successCount; }
        public int getFailCount() { return failCount; }
        public List<UserResult> getResults() { return results; }
        
        public static class UserResult {
            private final Long userId;
            private final boolean success;
            private final String message;
            
            public UserResult(Long userId, boolean success, String message) {
                this.userId = userId;
                this.success = success;
                this.message = message;
            }
            
            public Long getUserId() { return userId; }
            public boolean isSuccess() { return success; }
            public String getMessage() { return message; }
        }
    }
}