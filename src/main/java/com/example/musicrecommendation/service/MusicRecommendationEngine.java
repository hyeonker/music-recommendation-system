package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.UserProfile;
import com.example.musicrecommendation.web.dto.ProfileDto;
import com.example.musicrecommendation.service.UserBehaviorTrackingService;
import com.example.musicrecommendation.service.UserMusicPreferencesService;
import com.example.musicrecommendation.service.AdvancedCollaborativeFilteringService;
import com.example.musicrecommendation.service.MLRecommendationService;
import com.example.musicrecommendation.config.RecommendationProperties;
import com.example.musicrecommendation.web.dto.spotify.ArtistDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 고도화된 음악 추천 엔진
 * - 사용자 프로필 기반 개인화 추천
 * - 협업 필터링 (유사 사용자 기반)
 * - 컨텐츠 기반 필터링 (장르, 아티스트 유사성)
 * - 하이브리드 추천 알고리즘
 */
@Service
@RequiredArgsConstructor
public class MusicRecommendationEngine {
    
    private static final Logger log = LoggerFactory.getLogger(MusicRecommendationEngine.class);
    
    private final UserProfileService userProfileService;
    private final MusicMatchingService musicMatchingService;
    private final SpotifyService spotifyService;
    private final NotificationService notificationService;
    private final UserBehaviorTrackingService behaviorTrackingService;
    private final UserMusicPreferencesService preferencesService;
    private final AdvancedCollaborativeFilteringService collaborativeFilteringService;
    private final MLRecommendationService mlRecommendationService;
    private final RecommendationProperties properties;
    
    /**
     * 개인화된 음악 추천 (새로운 ML 기반 엔진)
     * @param userId 대상 사용자 ID
     * @param recommendationCount 추천할 곡 수
     * @return 추천 음악 리스트
     */
    @Cacheable(value = "musicRecommendations", key = "#userId + '_' + #recommendationCount", 
               unless = "#result == null || #result.isEmpty()")
    public List<Map<String, Object>> getPersonalizedRecommendations(Long userId, int recommendationCount) {
        try {
            log.info("사용자 {}에 대한 개선된 개인화 추천 생성 시작 ({}곡)", userId, recommendationCount);
            
            // A/B 테스트: 사용자 ID 기준으로 ML 모델 사용 여부 결정
            boolean useAdvancedAlgorithms = shouldUseAdvancedAlgorithms(userId);
            
            if (useAdvancedAlgorithms) {
                // 새로운 ML 기반 추천
                return getAdvancedPersonalizedRecommendations(userId, recommendationCount);
            } else {
                // 기존 하이브리드 추천 (fallback)
                return getLegacyPersonalizedRecommendations(userId, recommendationCount);
            }
            
        } catch (Exception e) {
            log.error("개인화 추천 생성 실패 (userId: {}): {}", userId, e.getMessage());
            return generateFallbackRecommendations(recommendationCount);
        }
    }
    
    /**
     * 새로운 고급 알고리즘 기반 추천
     */
    private List<Map<String, Object>> getAdvancedPersonalizedRecommendations(Long userId, int recommendationCount) {
        try {
            // 1. ML 기반 추천 (메인)
            List<MLRecommendationService.MLRecommendationItem> mlRecommendations = 
                mlRecommendationService.getMLBasedRecommendations(userId, recommendationCount);
            
            // 2. 고급 협업 필터링 추천 (보조)
            List<AdvancedCollaborativeFilteringService.RecommendationItem> cfRecommendations = 
                collaborativeFilteringService.getCollaborativeRecommendations(userId, recommendationCount / 2);
            
            // 3. ML 추천을 기본 포맷으로 변환
            List<Map<String, Object>> convertedMLRecs = mlRecommendations.stream()
                .map(this::convertMLRecommendationToMap)
                .collect(Collectors.toList());
            
            // 4. CF 추천을 기본 포맷으로 변환
            List<Map<String, Object>> convertedCFRecs = cfRecommendations.stream()
                .map(this::convertCFRecommendationToMap)
                .collect(Collectors.toList());
            
            // 5. 결합 및 최적화
            List<Map<String, Object>> combined = combineAdvancedRecommendations(
                convertedMLRecs, convertedCFRecs, recommendationCount);
            
            // 6. 메타데이터 추가
            var enhanced = enhanceRecommendationsWithMetadata(combined, userId);
            
            // 7. 추천 새로고침 이벤트 추적 (비동기)
            behaviorTrackingService.trackRefreshRecommendationsEvent(userId, null);
            
            log.info("사용자 {} 고급 알고리즘 추천 {} 개 생성 완료", userId, enhanced.size());
            return enhanced;
            
        } catch (Exception e) {
            log.warn("고급 알고리즘 추천 실패, 레거시로 폴백 (userId: {}): {}", userId, e.getMessage());
            return getLegacyPersonalizedRecommendations(userId, recommendationCount);
        }
    }
    
    /**
     * 기존 하이브리드 추천 (레거시)
     */
    private List<Map<String, Object>> getLegacyPersonalizedRecommendations(Long userId, int recommendationCount) {
        // 1. 사용자 프로필 가져오기
        var userProfile = userProfileService.getOrInit(userId);
        
        // 2. 기존 추천 알고리즘 사용
        List<Map<String, Object>> contentBasedRecs = generateContentBasedRecommendations(userProfile, recommendationCount / 3);
        List<Map<String, Object>> collaborativeRecs = generateCollaborativeRecommendations(userId, recommendationCount / 3);
        List<Map<String, Object>> trendingRecs = generateTrendingRecommendations(userProfile, recommendationCount / 3);
        
        // 3. 하이브리드 추천 생성
        List<Map<String, Object>> hybridRecommendations = combineRecommendations(
            contentBasedRecs, collaborativeRecs, trendingRecs, recommendationCount
        );
        
        // 4. 메타데이터 추가
        var finalRecommendations = enhanceRecommendationsWithMetadata(hybridRecommendations, userId);
        
        // 5. 추천 알림 전송 (비동기)
        try {
            if (!finalRecommendations.isEmpty()) {
                notificationService.sendRecommendationNotification(userId, "personalized", finalRecommendations.size());
            }
        } catch (Exception e) {
            // 알림 실패해도 추천은 정상 반환
        }
        
        return finalRecommendations;
    }
    
    /**
     * 컨텐츠 기반 필터링 추천
     * 사용자의 선호 장르, 아티스트를 기반으로 유사한 음악 추천
     */
    private List<Map<String, Object>> generateContentBasedRecommendations(ProfileDto userProfile, int count) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        try {
            // 선호 장르 추출
            List<String> preferredGenres = extractPreferredGenres(userProfile);
            
            // 선호 아티스트 추출
            List<String> favoriteArtists = extractFavoriteArtistNames(userProfile);
            
            // 장르 기반 추천곡 생성
            for (String genre : preferredGenres.stream().limit(2).toList()) {
                recommendations.addAll(generateGenreBasedTracks(genre, count / 2));
            }
            
            // 아티스트 유사성 기반 추천
            for (String artist : favoriteArtists.stream().limit(2).toList()) {
                recommendations.addAll(generateArtistSimilarTracks(artist, count / 2));
            }
            
        } catch (Exception e) {
            log.warn("컨텐츠 기반 추천 생성 실패: {}", e.getMessage());
        }
        
        return recommendations.stream().distinct().limit(count).collect(Collectors.toList());
    }
    
    /**
     * 협업 필터링 추천
     * 유사한 취향의 다른 사용자들이 좋아하는 음악 추천
     */
    private List<Map<String, Object>> generateCollaborativeRecommendations(Long userId, int count) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        try {
            // 유사한 사용자들 찾기
            var similarUsers = musicMatchingService.findMatchCandidates(userId, 5);
            
            for (var match : similarUsers) {
                Long similarUserId = match.getUser1Id().equals(userId) ? match.getUser2Id() : match.getUser1Id();
                var similarUserProfile = userProfileService.getOrInit(similarUserId);
                
                // 유사 사용자의 선호 음악에서 추천 생성
                recommendations.addAll(generateRecommendationsFromSimilarUser(similarUserProfile, count / 3));
            }
            
        } catch (Exception e) {
            log.warn("협업 필터링 추천 생성 실패: {}", e.getMessage());
        }
        
        return recommendations.stream().distinct().limit(count).collect(Collectors.toList());
    }
    
    /**
     * 트렌딩 기반 추천
     * 현재 인기있는 음악 중 사용자 취향에 맞는 것들 추천
     */
    private List<Map<String, Object>> generateTrendingRecommendations(ProfileDto userProfile, int count) {
        List<Map<String, Object>> trendingTracks = new ArrayList<>();
        
        try {
            List<String> preferredGenres = extractPreferredGenres(userProfile);
            
            // 장르별 트렌딩 곡 생성 (시뮬레이션)
            for (String genre : preferredGenres.stream().limit(3).toList()) {
                trendingTracks.addAll(generateTrendingTracksForGenre(genre, count / 3));
            }
            
        } catch (Exception e) {
            log.warn("트렌딩 추천 생성 실패: {}", e.getMessage());
        }
        
        return trendingTracks.stream().limit(count).collect(Collectors.toList());
    }
    
    /**
     * 추천 결과 결합 및 다양성 보장
     */
    private List<Map<String, Object>> combineRecommendations(
            List<Map<String, Object>> contentBased,
            List<Map<String, Object>> collaborative, 
            List<Map<String, Object>> trending,
            int targetCount) {
        
        List<Map<String, Object>> combined = new ArrayList<>();
        
        // 각 알고리즘에서 번갈아가며 선택하여 다양성 보장
        int maxSize = Math.max(Math.max(contentBased.size(), collaborative.size()), trending.size());
        
        for (int i = 0; i < maxSize && combined.size() < targetCount; i++) {
            if (i < contentBased.size()) combined.add(contentBased.get(i));
            if (i < collaborative.size() && combined.size() < targetCount) combined.add(collaborative.get(i));
            if (i < trending.size() && combined.size() < targetCount) combined.add(trending.get(i));
        }
        
        // 전면 개선: ID, 아티스트, 제목 기반 중복 제거
        return removeDuplicatesAndEnsureDiversity(combined, targetCount);
    }
    
    /**
     * 고도화된 중복 제거 및 다양성 보장
     */
    private List<Map<String, Object>> removeDuplicatesAndEnsureDiversity(
            List<Map<String, Object>> tracks, int targetCount) {
        
        Set<String> usedIds = new HashSet<>();
        Set<String> usedTitles = new HashSet<>();
        Map<String, Integer> artistCount = new HashMap<>();
        Map<String, Integer> genreCount = new HashMap<>();
        
        List<Map<String, Object>> diverseTracks = new ArrayList<>();
        
        // 가중치를 기반으로 정렬 (recommendationType에 따라)
        tracks.sort((a, b) -> {
            String typeA = (String) a.getOrDefault("recommendationType", "unknown");
            String typeB = (String) b.getOrDefault("recommendationType", "unknown");
            
            int priorityA = getRecommendationPriority(typeA);
            int priorityB = getRecommendationPriority(typeB);
            
            return Integer.compare(priorityB, priorityA); // 높은 우선순위 먼저
        });
        
        for (Map<String, Object> track : tracks) {
            if (diverseTracks.size() >= targetCount) break;
            
            String trackId = (String) track.get("id");
            String title = ((String) track.get("name")).toLowerCase().trim();
            String artist = (String) track.get("artist");
            String genre = (String) track.get("genre");
            
            // ID 중복 체크
            if (usedIds.contains(trackId)) continue;
            
            // 제목 유사성 체크 (유사한 제목 방지)
            boolean titleSimilar = usedTitles.stream()
                .anyMatch(usedTitle -> calculateStringSimilarity(title, usedTitle) > 0.8);
            if (titleSimilar) continue;
            
            // 아티스트 다양성 (동일 아티스트 최대 2곡)
            int currentArtistCount = artistCount.getOrDefault(artist, 0);
            if (currentArtistCount >= 2) continue;
            
            // 장르 다양성 (동일 장르 최대 절반)
            int currentGenreCount = genreCount.getOrDefault(genre, 0);
            int maxGenreCount = Math.max(1, targetCount / 3);
            if (currentGenreCount >= maxGenreCount) continue;
            
            // 통과한 트랙 추가
            diverseTracks.add(track);
            usedIds.add(trackId);
            usedTitles.add(title);
            artistCount.put(artist, currentArtistCount + 1);
            genreCount.put(genre, currentGenreCount + 1);
        }
        
        return diverseTracks;
    }
    
    /**
     * 추천 타입 별 우선순위 반환
     */
    private int getRecommendationPriority(String recommendationType) {
        return switch (recommendationType) {
            case "content_based" -> 10;
            case "artist_similarity" -> 9;
            case "collaborative" -> 8;
            case "trending" -> 6;
            case "fallback" -> 1;
            default -> 5;
        };
    }
    
    /**
     * 문자열 유사도 계산 (Levenshtein Distance 기반)
     */
    private double calculateStringSimilarity(String str1, String str2) {
        if (str1.equals(str2)) return 1.0;
        
        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0) return 1.0;
        
        int distance = levenshteinDistance(str1, str2);
        return 1.0 - (double) distance / maxLength;
    }
    
    /**
     * Levenshtein Distance 계산
     */
    private int levenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];
        
        for (int i = 0; i <= str1.length(); i++) {
            for (int j = 0; j <= str2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        Math.min(
                            dp[i - 1][j] + 1,        // deletion
                            dp[i][j - 1] + 1),       // insertion
                        dp[i - 1][j - 1] + (str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1) // substitution
                    );
                }
            }
        }
        
        return dp[str1.length()][str2.length()];
    }
    
    /**
     * 추천 결과에 개인화 메타데이터 추가
     */
    private List<Map<String, Object>> enhanceRecommendationsWithMetadata(
            List<Map<String, Object>> recommendations, Long userId) {
        
        return recommendations.stream().map(track -> {
            Map<String, Object> enhanced = new HashMap<>(track);
            enhanced.put("recommendedAt", Instant.now().toString());
            enhanced.put("recommendedFor", userId);
            enhanced.put("recommendationReason", generateRecommendationReason(track, userId));
            enhanced.put("personalityScore", calculatePersonalityScore(track, userId));
            return enhanced;
        }).collect(Collectors.toList());
    }
    
    /**
     * 장르별 매칭 점수 계산
     */
    public double calculateGenreCompatibility(ProfileDto user1Profile, ProfileDto user2Profile) {
        try {
            List<String> user1Genres = extractPreferredGenres(user1Profile);
            List<String> user2Genres = extractPreferredGenres(user2Profile);
            
            if (user1Genres.isEmpty() || user2Genres.isEmpty()) {
                return 0.5; // 기본 호환성
            }
            
            // 자카드 유사도 계산
            Set<String> intersection = new HashSet<>(user1Genres);
            intersection.retainAll(user2Genres);
            
            Set<String> union = new HashSet<>(user1Genres);
            union.addAll(user2Genres);
            
            return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
            
        } catch (Exception e) {
            log.warn("장르 호환성 계산 실패: {}", e.getMessage());
            return 0.5;
        }
    }
    
    /**
     * 아티스트 호환성 계산
     */
    public double calculateArtistCompatibility(ProfileDto user1Profile, ProfileDto user2Profile) {
        try {
            List<String> user1Artists = extractFavoriteArtistNames(user1Profile);
            List<String> user2Artists = extractFavoriteArtistNames(user2Profile);
            
            if (user1Artists.isEmpty() || user2Artists.isEmpty()) {
                return 0.3;
            }
            
            Set<String> commonArtists = new HashSet<>(user1Artists);
            commonArtists.retainAll(user2Artists);
            
            int totalArtists = Math.max(user1Artists.size(), user2Artists.size());
            return totalArtists > 0 ? (double) commonArtists.size() / totalArtists : 0.0;
            
        } catch (Exception e) {
            log.warn("아티스트 호환성 계산 실패: {}", e.getMessage());
            return 0.3;
        }
    }
    
    // Helper Methods
    
    private List<String> extractPreferredGenres(ProfileDto profile) {
        try {
            return profile.getFavoriteGenres().stream()
                    .map(genre -> (String) genre.get("name"))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            return List.of("Pop", "Rock"); // 기본 장르
        }
    }
    
    private List<String> extractFavoriteArtistNames(ProfileDto profile) {
        try {
            return profile.getFavoriteArtists().stream()
                    .map(artist -> (String) artist.get("name"))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            return List.of(); // 빈 목록
        }
    }
    
    private List<Map<String, Object>> generateGenreBasedTracks(String genre, int count) {
        List<Map<String, Object>> tracks = new ArrayList<>();
        
        // 장르별 대표 곡들 (다양성 보장)
        String[] trackTemplates = {
            "Essential " + genre + " Collection",
            "Modern " + genre + " Masterpiece",
            "Classic " + genre + " Revival",
            "Underground " + genre + " Gem",
            "Fresh " + genre + " Sound"
        };
        
        String[] artistTemplates = {
            genre + " Pioneer",
            "New Wave " + genre,
            genre + " Collective",
            "Rising " + genre + " Star",
            genre + " Innovator"
        };
        
        for (int i = 0; i < Math.min(count, trackTemplates.length); i++) {
            tracks.add(Map.of(
                "id", "genre_" + genre.toLowerCase().replaceAll("\\s+", "_") + "_" + System.nanoTime() + "_" + i,
                "name", trackTemplates[i],
                "artist", artistTemplates[i],
                "album", "Best of " + genre + " " + (2020 + i),
                "genre", genre,
                "duration", String.format("%d:%02d", 3 + (i % 3), 15 + (i * 13) % 50),
                "popularity", 75 + new Random().nextInt(20),
                "recommendationType", "content_based",
                "personalityScore", 0.8 + (i * 0.05)
            ));
        }
        
        return tracks;
    }
    
    private List<Map<String, Object>> generateArtistSimilarTracks(String artist, int count) {
        List<Map<String, Object>> tracks = new ArrayList<>();
        
        String[] similarityTypes = {
            "Style Inspired by " + artist,
            "Sound Like " + artist,
            "Influenced by " + artist + " Era",
            "Reminiscent of " + artist
        };
        
        for (int i = 0; i < Math.min(count, similarityTypes.length); i++) {
            String uniqueId = "similar_" + artist.replaceAll("\\s+", "_").toLowerCase() + "_" + System.nanoTime() + "_" + i;
            tracks.add(Map.of(
                "id", uniqueId,
                "name", similarityTypes[i],
                "artist", "Artist Similar to " + artist,
                "album", "Artists Like " + artist + " Collection",
                "genre", "Similar Style",
                "duration", String.format("%d:%02d", 3 + (i % 2), 20 + (i * 17) % 40),
                "popularity", 70 + new Random().nextInt(25),
                "recommendationType", "artist_similarity",
                "personalityScore", 0.9 // 아티스트 유사성은 가장 높은 개인화 점수
            ));
        }
        
        return tracks;
    }
    
    private List<Map<String, Object>> generateRecommendationsFromSimilarUser(ProfileDto similarUserProfile, int count) {
        List<String> genres = extractPreferredGenres(similarUserProfile);
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        for (String genre : genres.stream().limit(count).toList()) {
            recommendations.add(Map.of(
                "id", "collab_" + genre.toLowerCase() + "_" + System.currentTimeMillis(),
                "name", "Collaborative Pick: " + genre,
                "artist", "Similar User's Favorite",
                "album", "Community Recommendations",
                "genre", genre,
                "duration", "3:55",
                "popularity", 80,
                "recommendationType", "collaborative"
            ));
        }
        
        return recommendations;
    }
    
    private List<Map<String, Object>> generateTrendingTracksForGenre(String genre, int count) {
        List<Map<String, Object>> tracks = new ArrayList<>();
        tracks.add(Map.of(
            "id", "trending_" + genre.toLowerCase() + "_" + System.currentTimeMillis(),
            "name", "Trending " + genre + " Hit",
            "artist", "Chart Topper",
            "album", "Current Favorites",
            "genre", genre,
            "duration", "3:33",
            "popularity", 95,
            "recommendationType", "trending",
            "trending_rank", 1
        ));
        return tracks.stream().limit(count).toList();
    }
    
    private String generateRecommendationReason(Map<String, Object> track, Long userId) {
        String recType = (String) track.get("recommendationType");
        String genre = (String) track.get("genre");
        
        return switch (recType) {
            case "content_based" -> "선호하는 " + genre + " 장르 기반 추천";
            case "collaborative" -> "비슷한 취향의 사용자들이 좋아하는 곡";
            case "trending" -> "현재 인기 상승 중인 " + genre + " 음악";
            case "artist_similarity" -> "좋아하는 아티스트와 유사한 스타일";
            default -> "맞춤형 추천";
        };
    }
    
    private double calculatePersonalityScore(Map<String, Object> track, Long userId) {
        // 개인화 점수 계산 (0.0 ~ 1.0)
        String recType = (String) track.get("recommendationType");
        Integer popularity = (Integer) track.getOrDefault("popularity", 50);
        
        double baseScore = switch (recType) {
            case "content_based" -> 0.8;
            case "collaborative" -> 0.7;
            case "trending" -> 0.6;
            case "artist_similarity" -> 0.9;
            default -> 0.5;
        };
        
        // 인기도 조정
        double popularityFactor = popularity / 100.0;
        
        return Math.min(1.0, baseScore + (popularityFactor * 0.2));
    }
    
    private List<Map<String, Object>> generateFallbackRecommendations(int count) {
        log.info("폴백 추천 생성: {}곡", count);
        
        List<String> fallbackGenres = List.of("Pop", "Rock", "Hip-Hop", "Electronic", "R&B");
        List<Map<String, Object>> fallback = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            String genre = fallbackGenres.get(i % fallbackGenres.size());
            fallback.add(Map.of(
                "id", "fallback_" + i,
                "name", "Popular Song " + (i + 1),
                "artist", "Popular Artist",
                "album", "Greatest Hits",
                "genre", genre,
                "duration", "3:30",
                "popularity", 70,
                "recommendationType", "fallback"
            ));
        }
        
        return fallback;
    }
    
    /**
     * A/B 테스트: 고급 알고리즘 사용 여부 결정
     */
    private boolean shouldUseAdvancedAlgorithms(Long userId) {
        // 사용자 ID 기반 A/B 테스트 (50:50 분할)
        return userId % 2 == 0;
    }
    
    /**
     * ML 추천 결과를 기본 Map 포맷으로 변환
     */
    private Map<String, Object> convertMLRecommendationToMap(MLRecommendationService.MLRecommendationItem item) {
        Map<String, Object> converted = new HashMap<>();
        converted.put("id", item.getItemId());
        converted.put("name", "ML Recommended Track"); // 실제로는 메타데이터에서 가져옴
        converted.put("artist", "ML Artist");
        converted.put("album", "ML Album");
        converted.put("genre", "ML Genre");
        converted.put("duration", "3:45");
        converted.put("popularity", (int) (item.getScore() * 100));
        converted.put("recommendationType", "ml_based");
        converted.put("personalityScore", item.getScore());
        converted.put("confidence", item.getConfidence());
        converted.put("algorithm", item.getAlgorithm());
        converted.put("mlFeatures", item.getFeatures());
        return converted;
    }
    
    /**
     * 협업 필터링 추천 결과를 기본 Map 포맷으로 변환
     */
    private Map<String, Object> convertCFRecommendationToMap(AdvancedCollaborativeFilteringService.RecommendationItem item) {
        Map<String, Object> converted = new HashMap<>();
        converted.put("id", item.getItemId());
        converted.put("name", "CF Recommended Track"); // 실제로는 메타데이터에서 가져옴
        converted.put("artist", "CF Artist");
        converted.put("album", "CF Album");
        converted.put("genre", "CF Genre");
        converted.put("duration", "3:40");
        converted.put("popularity", (int) (item.getScore() * 100));
        converted.put("recommendationType", "advanced_cf");
        converted.put("personalityScore", item.getScore());
        converted.put("confidence", item.getConfidence());
        converted.put("algorithm", item.getAlgorithm());
        return converted;
    }
    
    /**
     * 고급 추천 결과 결합
     */
    private List<Map<String, Object>> combineAdvancedRecommendations(
            List<Map<String, Object>> mlRecommendations,
            List<Map<String, Object>> cfRecommendations,
            int targetCount) {
        
        Map<String, Map<String, Object>> combined = new HashMap<>();
        
        // ML 추천 (높은 가중치: 0.7)
        for (Map<String, Object> item : mlRecommendations) {
            String itemId = (String) item.get("id");
            Map<String, Object> weighted = new HashMap<>(item);
            double score = (Double) item.get("personalityScore");
            weighted.put("personalityScore", score * 0.7);
            combined.put(itemId, weighted);
        }
        
        // CF 추천 (낮은 가중치: 0.3, 또는 기존 점수와 결합)
        for (Map<String, Object> item : cfRecommendations) {
            String itemId = (String) item.get("id");
            if (combined.containsKey(itemId)) {
                // 이미 존재하는 경우 점수 결합
                Map<String, Object> existing = combined.get(itemId);
                double existingScore = (Double) existing.get("personalityScore");
                double newScore = (Double) item.get("personalityScore");
                existing.put("personalityScore", existingScore + (newScore * 0.3));
                existing.put("algorithm", "HYBRID_ML_CF");
            } else {
                // 새로운 항목
                Map<String, Object> weighted = new HashMap<>(item);
                double score = (Double) item.get("personalityScore");
                weighted.put("personalityScore", score * 0.3);
                combined.put(itemId, weighted);
            }
        }
        
        // 점수 기준 정렬 후 반환
        return combined.values().stream()
            .sorted((a, b) -> Double.compare(
                (Double) b.get("personalityScore"), 
                (Double) a.get("personalityScore")
            ))
            .limit(targetCount)
            .collect(Collectors.toList());
    }
}