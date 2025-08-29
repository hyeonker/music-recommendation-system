package com.example.musicrecommendation.web.dto;

import com.example.musicrecommendation.domain.UserBadge;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.Map;

public class BadgeDto {
    
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private Long userId;
        private UserBadge.BadgeType badgeType;
        private String badgeName;
        private String description;
        private String iconUrl;
        private String rarity;
        private String badgeColor;
        private OffsetDateTime earnedAt;
        private Map<String, Object> metadata;
        
        public static Response from(UserBadge badge) {
            return Response.builder()
                    .id(badge.getId())
                    .userId(badge.getUserId())
                    .badgeType(badge.getBadgeType())
                    .badgeName(badge.getBadgeName())
                    .description(badge.getDescription())
                    .iconUrl(badge.getIconUrl())
                    .rarity(badge.getRarity())
                    .badgeColor(badge.getBadgeColor())
                    .earnedAt(badge.getEarnedAt())
                    .metadata(badge.getMetadata())
                    .build();
        }
    }
    
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserBadgeProfile {
        private Long userId;
        private String userNickname;
        private Long totalBadges;
        private Long commonBadges;
        private Long uncommonBadges;
        private Long rareBadges;
        private Long epicBadges;
        private Long legendaryBadges;
        private java.util.List<Response> recentBadges;
        private java.util.List<Response> featuredBadges;
    }
    
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BadgeStatistics {
        private UserBadge.BadgeType badgeType;
        private String badgeName;
        private Long totalEarned;
        private String rarity;
        private Double earningRate; // 전체 사용자 대비 획득 비율
    }
}