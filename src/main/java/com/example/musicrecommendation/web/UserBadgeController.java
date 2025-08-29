package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.UserBadgeService;
import com.example.musicrecommendation.web.dto.BadgeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
@Slf4j
public class UserBadgeController {

    private final UserBadgeService badgeService;
    
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
            @AuthenticationPrincipal OAuth2User oauth2User) {
        
        Long userId = getUserId(oauth2User);
        
        var badges = badgeService.getUserBadges(userId)
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
                        case BETA_TESTER, ANNIVERSARY, SPECIAL_EVENT -> "LEGENDARY";
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
    
    private Long getUserId(OAuth2User oauth2User) {
        // OAuth2User에서 실제 사용자 ID 추출 로직
        // 현재는 임시로 1L 반환, 실제 구현 시 수정 필요
        return 1L; // TODO: 실제 사용자 ID 추출 로직 구현
    }
}