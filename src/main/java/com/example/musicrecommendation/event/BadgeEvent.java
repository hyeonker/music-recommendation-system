package com.example.musicrecommendation.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BadgeEvent {
    private final Long userId;
    private final String action; // "CREATED", "EXPIRED", "DELETED"
    private final String badgeName;
    
    public static BadgeEvent created(Long userId, String badgeName) {
        return new BadgeEvent(userId, "CREATED", badgeName);
    }
    
    public static BadgeEvent expired(Long userId, String badgeName) {
        return new BadgeEvent(userId, "EXPIRED", badgeName);
    }
    
    public static BadgeEvent deleted(Long userId, String badgeName) {
        return new BadgeEvent(userId, "DELETED", badgeName);
    }
}