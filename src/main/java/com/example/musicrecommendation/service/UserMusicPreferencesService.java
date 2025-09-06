package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.UserBehaviorEvent;
import com.example.musicrecommendation.domain.UserMusicPreferences;
import com.example.musicrecommendation.repository.UserBehaviorEventRepository;
import com.example.musicrecommendation.repository.UserMusicPreferencesRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 사용자 음악 취향 프로필 관리 서비스
 * - 행동 데이터 기반 취향 분석
 * - 실시간 선호도 계산 및 업데이트
 * - 협업 필터링을 위한 유사도 계산
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMusicPreferencesService {
    
    private final UserMusicPreferencesRepository preferencesRepository;
    private final UserBehaviorEventRepository behaviorRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 사용자 취향 프로필 조회 (캐시됨)
     */
    @Cacheable(value = "userMusicPreferences", key = "#userId")
    @Transactional(readOnly = true)
    public UserMusicPreferences getUserPreferences(Long userId) {
        return preferencesRepository.findByUserId(userId)
            .orElseGet(() -> createInitialPreferences(userId));
    }
    
    /**
     * 사용자 취향 프로필 비동기 업데이트
     */
    @Async
    @Transactional
    @CacheEvict(value = {"userMusicPreferences", "musicRecommendations"}, key = "#userId")
    public CompletableFuture<UserMusicPreferences> updateUserPreferencesAsync(Long userId) {
        return CompletableFuture.supplyAsync(() -> updateUserPreferences(userId));
    }
    
    /**
     * 사용자 취향 프로필 동기 업데이트
     */
    @Transactional
    @CacheEvict(value = {"userMusicPreferences", "musicRecommendations"}, key = "#userId")
    public UserMusicPreferences updateUserPreferences(Long userId) {
        try {
            log.debug("사용자 {} 취향 프로필 업데이트 시작", userId);
            
            // 최근 30일간의 행동 데이터 분석
            LocalDateTime since = LocalDateTime.now().minusDays(30);
            List<UserBehaviorEvent> recentEvents = behaviorRepository.findRecentUserEvents(userId, since);
            
            if (recentEvents.isEmpty()) {
                return getUserPreferences(userId); // 기존 프로필 반환
            }
            
            // 선호도 계산
            JsonNode genrePreferences = calculateGenrePreferences(recentEvents);
            JsonNode artistPreferences = calculateArtistPreferences(recentEvents);
            JsonNode audioFeatures = calculateAudioFeaturePreferences(recentEvents);
            JsonNode listeningPatterns = calculateListeningPatterns(recentEvents);
            
            // 기존 프로필 조회 또는 생성
            UserMusicPreferences preferences = preferencesRepository.findByUserId(userId)
                .orElse(UserMusicPreferences.builder().userId(userId).build());
            
            // 업데이트
            preferences.setGenrePreferences(genrePreferences);
            preferences.setArtistPreferences(artistPreferences);
            preferences.setAudioFeatures(audioFeatures);
            preferences.setListeningPatterns(listeningPatterns);
            preferences.setLastUpdated(LocalDateTime.now());
            
            UserMusicPreferences saved = preferencesRepository.save(preferences);
            log.debug("사용자 {} 취향 프로필 업데이트 완료", userId);
            
            return saved;
            
        } catch (Exception e) {
            log.error("사용자 {} 취향 프로필 업데이트 실패: {}", userId, e.getMessage());
            return getUserPreferences(userId); // 기존 프로필 반환
        }
    }
    
    /**
     * 장르 선호도 계산
     */
    private JsonNode calculateGenrePreferences(List<UserBehaviorEvent> events) {
        Map<String, Double> genreScores = new HashMap<>();
        Map<String, Integer> genreCounts = new HashMap<>();
        
        for (UserBehaviorEvent event : events) {
            if (event.getItemMetadata() != null && event.getItemMetadata().has("genre")) {
                String genre = event.getItemMetadata().get("genre").asText().toLowerCase();
                double score = event.getImplicitFeedbackScore();
                
                genreScores.merge(genre, score, Double::sum);
                genreCounts.merge(genre, 1, Integer::sum);
            }
        }
        
        // 정규화된 평균 점수 계산
        ObjectNode result = objectMapper.createObjectNode();
        genreScores.forEach((genre, totalScore) -> {
            double avgScore = totalScore / genreCounts.get(genre);
            // 0.0 ~ 1.0 범위로 정규화
            double normalizedScore = Math.max(0.0, Math.min(1.0, avgScore));
            result.put(genre, normalizedScore);
        });
        
        return result;
    }
    
    /**
     * 아티스트 선호도 계산
     */
    private JsonNode calculateArtistPreferences(List<UserBehaviorEvent> events) {
        Map<String, Double> artistScores = new HashMap<>();
        Map<String, Integer> artistCounts = new HashMap<>();
        
        for (UserBehaviorEvent event : events) {
            if (event.getItemMetadata() != null && event.getItemMetadata().has("artist")) {
                String artist = event.getItemMetadata().get("artist").asText();
                double score = event.getImplicitFeedbackScore();
                
                artistScores.merge(artist, score, Double::sum);
                artistCounts.merge(artist, 1, Integer::sum);
            }
        }
        
        // 상위 50개 아티스트만 저장 (성능 최적화)
        ObjectNode result = objectMapper.createObjectNode();
        artistScores.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(
                e2.getValue() / artistCounts.get(e2.getKey()),
                e1.getValue() / artistCounts.get(e1.getKey())
            ))
            .limit(50)
            .forEach(entry -> {
                String artist = entry.getKey();
                double avgScore = entry.getValue() / artistCounts.get(artist);
                double normalizedScore = Math.max(0.0, Math.min(1.0, avgScore));
                result.put(artist, normalizedScore);
            });
        
        return result;
    }
    
    /**
     * 음악적 특성 선호도 계산
     */
    private JsonNode calculateAudioFeaturePreferences(List<UserBehaviorEvent> events) {
        List<Double> tempos = new ArrayList<>();
        List<Double> energies = new ArrayList<>();
        List<Double> danceabilities = new ArrayList<>();
        List<Double> valences = new ArrayList<>();
        
        for (UserBehaviorEvent event : events) {
            if (event.isPositiveEvent() && event.getItemMetadata() != null) {
                JsonNode metadata = event.getItemMetadata();
                if (metadata.has("tempo")) tempos.add(metadata.get("tempo").asDouble());
                if (metadata.has("energy")) energies.add(metadata.get("energy").asDouble());
                if (metadata.has("danceability")) danceabilities.add(metadata.get("danceability").asDouble());
                if (metadata.has("valence")) valences.add(metadata.get("valence").asDouble());
            }
        }
        
        ObjectNode result = objectMapper.createObjectNode();
        if (!tempos.isEmpty()) {
            result.put("tempo", tempos.stream().mapToDouble(d -> d).average().orElse(120.0));
        }
        if (!energies.isEmpty()) {
            result.put("energy", energies.stream().mapToDouble(d -> d).average().orElse(0.5));
        }
        if (!danceabilities.isEmpty()) {
            result.put("danceability", danceabilities.stream().mapToDouble(d -> d).average().orElse(0.5));
        }
        if (!valences.isEmpty()) {
            result.put("valence", valences.stream().mapToDouble(d -> d).average().orElse(0.5));
        }
        
        return result;
    }
    
    /**
     * 활동 패턴 분석 (실제 가능한 데이터 기반)
     */
    private JsonNode calculateListeningPatterns(List<UserBehaviorEvent> events) {
        Map<Integer, Integer> hourCounts = new HashMap<>();
        Map<Integer, Integer> dayOfWeekCounts = new HashMap<>();
        int totalSessions = 0;
        double totalEngagement = 0.0;
        
        for (UserBehaviorEvent event : events) {
            // 활발한 활동만 패턴 분석에 포함 (클릭, 좋아요, 검색 등)
            if (event.isPositiveEvent() || event.getEventType() == UserBehaviorEvent.EventType.SEARCH ||
                event.getEventType() == UserBehaviorEvent.EventType.SESSION_START) {
                
                LocalDateTime dateTime = event.getCreatedAt();
                hourCounts.merge(dateTime.getHour(), 1, Integer::sum);
                dayOfWeekCounts.merge(dateTime.getDayOfWeek().getValue(), 1, Integer::sum);
                totalEngagement += event.getImplicitFeedbackScore();
                
                if (event.getEventType() == UserBehaviorEvent.EventType.SESSION_START) {
                    totalSessions++;
                }
            }
        }
        
        ObjectNode result = objectMapper.createObjectNode();
        
        // 가장 활발한 시간대 (상위 3개)
        List<Integer> peakHours = hourCounts.entrySet().stream()
            .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        result.set("peak_hours", objectMapper.valueToTree(peakHours));
        
        // 주말 활동 선호도 (주말 활동률)
        int weekendActivity = dayOfWeekCounts.getOrDefault(6, 0) + dayOfWeekCounts.getOrDefault(7, 0);
        int totalActivity = dayOfWeekCounts.values().stream().mapToInt(Integer::intValue).sum();
        double weekendPreference = totalActivity > 0 ? (double) weekendActivity / totalActivity : 0.0;
        result.put("weekend_preference", weekendPreference);
        
        // 평균 세션당 참여도
        double avgEngagementPerSession = totalSessions > 0 ? totalEngagement / totalSessions : 0.0;
        result.put("avg_engagement_per_session", avgEngagementPerSession);
        
        // 활동 빈도 (일일 평균 활동 수)
        long daySpan = events.isEmpty() ? 1 : 
            java.time.Duration.between(
                events.get(events.size() - 1).getCreatedAt(),
                events.get(0).getCreatedAt()
            ).toDays() + 1;
        double dailyActivityRate = (double) totalActivity / daySpan;
        result.put("daily_activity_rate", dailyActivityRate);
        
        return result;
    }
    
    /**
     * 초기 프로필 생성
     */
    private UserMusicPreferences createInitialPreferences(Long userId) {
        ObjectNode emptyPreferences = objectMapper.createObjectNode();
        
        return UserMusicPreferences.builder()
            .userId(userId)
            .genrePreferences(emptyPreferences)
            .artistPreferences(emptyPreferences)
            .audioFeatures(emptyPreferences)
            .listeningPatterns(emptyPreferences)
            .build();
    }
    
    /**
     * 유사한 취향의 사용자 찾기
     */
    @Transactional(readOnly = true)
    public List<Long> findSimilarUsers(Long userId, int limit) {
        try {
            List<Object[]> similarUsers = preferencesRepository.findSimilarUsersByGenre(userId, limit);
            return similarUsers.stream()
                .map(row -> ((Number) row[1]).longValue()) // user2_id
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("유사 사용자 검색 실패 (userId: {}): {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 특정 장르를 선호하는 사용자들 찾기
     */
    @Transactional(readOnly = true)
    public List<Long> findUsersLikeGenre(String genre, double minScore, int limit) {
        try {
            List<Object[]> users = preferencesRepository.findUsersWithGenrePreference(
                genre.toLowerCase(), minScore, limit);
            return users.stream()
                .map(row -> ((Number) row[0]).longValue())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("장르 {} 선호 사용자 검색 실패: {}", genre, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 두 사용자 간 취향 유사도 계산
     */
    @Transactional(readOnly = true)
    public double calculateUserSimilarity(Long user1Id, Long user2Id) {
        try {
            UserMusicPreferences prefs1 = getUserPreferences(user1Id);
            UserMusicPreferences prefs2 = getUserPreferences(user2Id);
            
            if (prefs1.getGenrePreferences() == null || prefs2.getGenrePreferences() == null) {
                return 0.0;
            }
            
            return calculateCosineSimilarity(prefs1.getGenrePreferences(), prefs2.getGenrePreferences());
            
        } catch (Exception e) {
            log.warn("사용자 유사도 계산 실패 ({}, {}): {}", user1Id, user2Id, e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * 코사인 유사도 계산 (JSONB 기반)
     */
    private double calculateCosineSimilarity(JsonNode prefs1, JsonNode prefs2) {
        Set<String> commonGenres = new HashSet<>();
        prefs1.fieldNames().forEachRemaining(commonGenres::add);
        
        Set<String> prefs2Genres = new HashSet<>();
        prefs2.fieldNames().forEachRemaining(prefs2Genres::add);
        
        commonGenres.retainAll(prefs2Genres);
        
        if (commonGenres.isEmpty()) return 0.0;
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (String genre : commonGenres) {
            double score1 = prefs1.get(genre).asDouble();
            double score2 = prefs2.get(genre).asDouble();
            
            dotProduct += score1 * score2;
            norm1 += score1 * score1;
            norm2 += score2 * score2;
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) return 0.0;
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}