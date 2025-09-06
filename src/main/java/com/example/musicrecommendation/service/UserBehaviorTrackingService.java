package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.UserBehaviorEvent;
import com.example.musicrecommendation.repository.UserBehaviorEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 실시간 사용자 행동 추적 서비스
 * - 모든 사용자 인터랙션 추적
 * - 암시적 피드백 수집
 * - 실시간 선호도 업데이트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBehaviorTrackingService {
    
    private final UserBehaviorEventRepository behaviorRepository;
    private final UserMusicPreferencesService preferencesService;
    private final ObjectMapper objectMapper;
    
    /**
     * 사용자 행동 이벤트 기록 (비동기)
     */
    @Async
    @Transactional
    @CacheEvict(value = "musicRecommendations", key = "#userId + '_*'")
    public CompletableFuture<Void> trackUserBehavior(Long userId, 
                                                    UserBehaviorEvent.EventType eventType,
                                                    UserBehaviorEvent.ItemType itemType,
                                                    String itemId,
                                                    Map<String, Object> metadata,
                                                    String sessionId,
                                                    Integer durationSeconds,
                                                    UserBehaviorEvent.DeviceType deviceType) {
        return CompletableFuture.runAsync(() -> {
            try {
                JsonNode metadataJson = metadata != null ? 
                    objectMapper.valueToTree(metadata) : null;
                
                UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .eventType(eventType)
                    .itemType(itemType)
                    .itemId(itemId)
                    .itemMetadata(metadataJson)
                    .sessionId(sessionId != null ? sessionId : UUID.randomUUID().toString())
                    .durationSeconds(durationSeconds)
                    .deviceType(deviceType != null ? deviceType : UserBehaviorEvent.DeviceType.WEB)
                    .build();
                
                behaviorRepository.save(event);
                
                // 중요한 이벤트의 경우 즉시 선호도 업데이트 트리거
                if (event.isPositiveEvent() || event.isNegativeEvent()) {
                    preferencesService.updateUserPreferencesAsync(userId);
                }
                
                log.debug("사용자 {} 행동 이벤트 추적: {} - {}", userId, eventType, itemId);
                
            } catch (Exception e) {
                log.error("사용자 행동 추적 실패 (userId: {}, eventType: {}): {}", 
                         userId, eventType, e.getMessage());
            }
        });
    }
    
    /**
     * 추천 곡 클릭 이벤트 추적 (YouTube/Spotify 이동)
     */
    public CompletableFuture<Void> trackRecommendationClick(Long userId, String trackId, 
                                                           Map<String, Object> trackMetadata,
                                                           String sessionId,
                                                           String platform) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        if (trackMetadata != null) metadata.putAll(trackMetadata);
        metadata.put("platform", platform); // "youtube" or "spotify"
        
        return trackUserBehavior(
            userId, 
            UserBehaviorEvent.EventType.RECOMMENDATION_CLICK,
            UserBehaviorEvent.ItemType.SONG,
            trackId,
            metadata,
            sessionId,
            null,
            UserBehaviorEvent.DeviceType.WEB
        );
    }
    
    /**
     * 좋아요/싫어요 이벤트 추적
     */
    public CompletableFuture<Void> trackLikeEvent(Long userId, String itemId, 
                                                 UserBehaviorEvent.ItemType itemType,
                                                 Map<String, Object> metadata,
                                                 boolean isLike) {
        return trackUserBehavior(
            userId,
            isLike ? UserBehaviorEvent.EventType.LIKE : UserBehaviorEvent.EventType.DISLIKE,
            itemType,
            itemId,
            metadata,
            null,
            null,
            UserBehaviorEvent.DeviceType.WEB
        );
    }
    
    /**
     * 플레이리스트 추가 이벤트 추적
     */
    public CompletableFuture<Void> trackPlaylistAddEvent(Long userId, String trackId,
                                                        Map<String, Object> trackMetadata,
                                                        String sessionId) {
        return trackUserBehavior(
            userId,
            UserBehaviorEvent.EventType.PLAYLIST_ADD,
            UserBehaviorEvent.ItemType.SONG,
            trackId,
            trackMetadata,
            sessionId,
            null,
            UserBehaviorEvent.DeviceType.WEB
        );
    }
    
    /**
     * 리뷰 작성 이벤트 추적
     */
    public CompletableFuture<Void> trackReviewEvent(Long userId, String itemId,
                                                   UserBehaviorEvent.ItemType itemType,
                                                   double rating, String reviewText) {
        Map<String, Object> metadata = Map.of(
            "rating", rating,
            "hasText", reviewText != null && !reviewText.trim().isEmpty(),
            "textLength", reviewText != null ? reviewText.length() : 0
        );
        
        return trackUserBehavior(
            userId,
            UserBehaviorEvent.EventType.REVIEW_WRITE,
            itemType,
            itemId,
            metadata,
            null,
            null,
            UserBehaviorEvent.DeviceType.WEB
        );
    }
    
    /**
     * 프로필 업데이트 이벤트 추적
     */
    public CompletableFuture<Void> trackProfileUpdateEvent(Long userId, String updateType,
                                                          Map<String, Object> updateData) {
        return trackUserBehavior(
            userId,
            UserBehaviorEvent.EventType.PROFILE_UPDATE,
            UserBehaviorEvent.ItemType.GENRE,
            updateType,
            updateData,
            null,
            null,
            UserBehaviorEvent.DeviceType.WEB
        );
    }
    
    /**
     * 세션 시작/종료 이벤트 추적
     */
    public CompletableFuture<Void> trackSessionEvent(Long userId, String sessionId, boolean isStart) {
        return trackUserBehavior(
            userId,
            isStart ? UserBehaviorEvent.EventType.SESSION_START : UserBehaviorEvent.EventType.SESSION_END,
            UserBehaviorEvent.ItemType.SONG,
            "session",
            null,
            sessionId,
            null,
            UserBehaviorEvent.DeviceType.WEB
        );
    }
    
    /**
     * 추천 새로고침 이벤트 추적
     */
    public CompletableFuture<Void> trackRefreshRecommendationsEvent(Long userId, String sessionId) {
        return trackUserBehavior(
            userId,
            UserBehaviorEvent.EventType.REFRESH_RECOMMENDATIONS,
            UserBehaviorEvent.ItemType.SONG,
            "refresh",
            null,
            sessionId,
            null,
            UserBehaviorEvent.DeviceType.WEB
        );
    }
    
    /**
     * 검색 이벤트 추적
     */
    public CompletableFuture<Void> trackSearchEvent(Long userId, String searchQuery,
                                                   UserBehaviorEvent.ItemType searchType,
                                                   String sessionId) {
        Map<String, Object> metadata = Map.of("query", searchQuery);
        return trackUserBehavior(
            userId,
            UserBehaviorEvent.EventType.SEARCH,
            searchType,
            searchQuery,
            metadata,
            sessionId,
            null,
            UserBehaviorEvent.DeviceType.WEB
        );
    }
    
    /**
     * 사용자의 최근 긍정적 행동 조회
     */
    @Transactional(readOnly = true)
    public List<UserBehaviorEvent> getRecentPositiveEvents(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return behaviorRepository.findPositiveUserEvents(userId, since);
    }
    
    /**
     * 사용자의 최근 행동 패턴 분석
     */
    @Transactional(readOnly = true)
    public UserBehaviorAnalysis analyzeUserBehavior(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<UserBehaviorEvent> events = behaviorRepository.findRecentUserEvents(userId, since);
        
        if (events.isEmpty()) {
            return UserBehaviorAnalysis.empty();
        }
        
        return UserBehaviorAnalysis.builder()
            .totalEvents(events.size())
            .positiveEvents((int) events.stream().filter(UserBehaviorEvent::isPositiveEvent).count())
            .negativeEvents((int) events.stream().filter(UserBehaviorEvent::isNegativeEvent).count())
            .averageSessionLength(calculateAverageSessionLength(events))
            .mostActiveHour(findMostActiveHour(events))
            .engagementScore(calculateEngagementScore(events))
            .build();
    }
    
    /**
     * 세션별 사용자 행동 조회
     */
    @Transactional(readOnly = true)
    public List<UserBehaviorEvent> getSessionEvents(String sessionId) {
        return behaviorRepository.findBySessionIdOrderByCreatedAt(sessionId);
    }
    
    private double calculateAverageSessionLength(List<UserBehaviorEvent> events) {
        Map<String, List<UserBehaviorEvent>> sessionGroups = events.stream()
            .filter(e -> e.getSessionId() != null)
            .collect(java.util.stream.Collectors.groupingBy(UserBehaviorEvent::getSessionId));
            
        if (sessionGroups.isEmpty()) return 0.0;
        
        double totalSessionLength = sessionGroups.values().stream()
            .mapToDouble(sessionEvents -> {
                if (sessionEvents.size() < 2) return 0.0;
                LocalDateTime start = sessionEvents.get(0).getCreatedAt();
                LocalDateTime end = sessionEvents.get(sessionEvents.size() - 1).getCreatedAt();
                return java.time.Duration.between(start, end).toMinutes();
            })
            .sum();
            
        return totalSessionLength / sessionGroups.size();
    }
    
    private int findMostActiveHour(List<UserBehaviorEvent> events) {
        Map<Integer, Long> hourCounts = events.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                e -> e.getCreatedAt().getHour(),
                java.util.stream.Collectors.counting()
            ));
            
        return hourCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(18); // 기본값: 저녁 6시
    }
    
    private double calculateEngagementScore(List<UserBehaviorEvent> events) {
        if (events.isEmpty()) return 0.0;
        
        double totalScore = events.stream()
            .mapToDouble(UserBehaviorEvent::getImplicitFeedbackScore)
            .sum();
            
        return totalScore / events.size();
    }
    
    /**
     * 사용자 행동 분석 결과 DTO
     */
    public static class UserBehaviorAnalysis {
        private final int totalEvents;
        private final int positiveEvents;
        private final int negativeEvents;
        private final double averageSessionLength;
        private final int mostActiveHour;
        private final double engagementScore;
        
        private UserBehaviorAnalysis(int totalEvents, int positiveEvents, int negativeEvents,
                                   double averageSessionLength, int mostActiveHour, double engagementScore) {
            this.totalEvents = totalEvents;
            this.positiveEvents = positiveEvents;
            this.negativeEvents = negativeEvents;
            this.averageSessionLength = averageSessionLength;
            this.mostActiveHour = mostActiveHour;
            this.engagementScore = engagementScore;
        }
        
        public static UserBehaviorAnalysisBuilder builder() {
            return new UserBehaviorAnalysisBuilder();
        }
        
        public static UserBehaviorAnalysis empty() {
            return new UserBehaviorAnalysis(0, 0, 0, 0.0, 18, 0.0);
        }
        
        // Getters
        public int getTotalEvents() { return totalEvents; }
        public int getPositiveEvents() { return positiveEvents; }
        public int getNegativeEvents() { return negativeEvents; }
        public double getAverageSessionLength() { return averageSessionLength; }
        public int getMostActiveHour() { return mostActiveHour; }
        public double getEngagementScore() { return engagementScore; }
        public double getPositivityRatio() {
            return totalEvents > 0 ? (double) positiveEvents / totalEvents : 0.0;
        }
        
        public static class UserBehaviorAnalysisBuilder {
            private int totalEvents;
            private int positiveEvents;
            private int negativeEvents;
            private double averageSessionLength;
            private int mostActiveHour;
            private double engagementScore;
            
            public UserBehaviorAnalysisBuilder totalEvents(int totalEvents) {
                this.totalEvents = totalEvents;
                return this;
            }
            
            public UserBehaviorAnalysisBuilder positiveEvents(int positiveEvents) {
                this.positiveEvents = positiveEvents;
                return this;
            }
            
            public UserBehaviorAnalysisBuilder negativeEvents(int negativeEvents) {
                this.negativeEvents = negativeEvents;
                return this;
            }
            
            public UserBehaviorAnalysisBuilder averageSessionLength(double averageSessionLength) {
                this.averageSessionLength = averageSessionLength;
                return this;
            }
            
            public UserBehaviorAnalysisBuilder mostActiveHour(int mostActiveHour) {
                this.mostActiveHour = mostActiveHour;
                return this;
            }
            
            public UserBehaviorAnalysisBuilder engagementScore(double engagementScore) {
                this.engagementScore = engagementScore;
                return this;
            }
            
            public UserBehaviorAnalysis build() {
                return new UserBehaviorAnalysis(totalEvents, positiveEvents, negativeEvents,
                                              averageSessionLength, mostActiveHour, engagementScore);
            }
        }
    }
}