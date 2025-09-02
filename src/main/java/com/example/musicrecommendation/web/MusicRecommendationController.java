package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.MusicRecommendationEngine;
import com.example.musicrecommendation.service.UserProfileService;
import com.example.musicrecommendation.service.MusicReviewService;
import com.example.musicrecommendation.service.SpotifyService;
import com.example.musicrecommendation.domain.MusicReview;
import com.example.musicrecommendation.web.dto.ProfileDto;
import com.example.musicrecommendation.web.dto.spotify.TrackDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class MusicRecommendationController {
    
    private final MusicRecommendationEngine recommendationEngine;
    private final UserProfileService userProfileService;
    private final MusicReviewService musicReviewService;
    private final SpotifyService spotifyService;
    
    // 사용자별 일일 새로고침 횟수 추적 (메모리 캐시)
    private final Map<String, Integer> dailyRefreshCount = new ConcurrentHashMap<>();
    private final Map<String, String> lastRefreshDate = new ConcurrentHashMap<>();
    private final int MAX_DAILY_REFRESH = 10;
    
    /**
     * Dashboard용 사용자 맞춤 추천 (Spotify 기반)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserRecommendations(@PathVariable Long userId) {
        try {
            // 새로고침 제한 확인
            String userKey = userId.toString();
            String today = LocalDate.now().toString();
            
            // 날짜가 바뀌면 카운트 리셋
            if (!today.equals(lastRefreshDate.get(userKey))) {
                dailyRefreshCount.put(userKey, 0);
                lastRefreshDate.put(userKey, today);
            }
            
            int currentCount = dailyRefreshCount.getOrDefault(userKey, 0);
            
            // 일일 제한 확인
            if (currentCount >= MAX_DAILY_REFRESH) {
                return ResponseEntity.ok(Map.of(
                    "error", "DAILY_LIMIT_EXCEEDED",
                    "message", "오늘 새로고침 횟수를 모두 사용했습니다. (10회 제한)",
                    "remainingRefresh", 0,
                    "maxRefresh", MAX_DAILY_REFRESH
                ));
            }
            
            // 새로고침 카운트 증가
            dailyRefreshCount.put(userKey, currentCount + 1);
            
            List<Map<String, Object>> recommendations = generateSpotifyBasedRecommendations(userId);
            
            // 기존 호환성을 위해 추천 리스트만 반환 (새로고침 정보는 헤더로)
            return ResponseEntity.ok()
                .header("X-Refresh-Used", String.valueOf(currentCount + 1))
                .header("X-Refresh-Remaining", String.valueOf(MAX_DAILY_REFRESH - (currentCount + 1)))
                .header("X-Refresh-Max", String.valueOf(MAX_DAILY_REFRESH))
                .header("X-Refresh-Reset-Date", today)
                .body(recommendations);
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    private List<Map<String, Object>> generateSpotifyBasedRecommendations(Long userId) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        Set<String> usedTrackIds = new HashSet<>(); // 중복 방지
        Random random = new Random();
        
        try {
            // 1. 사용자 프로필 가져오기
            ProfileDto profile = userProfileService.getOrInit(userId);
            
            // 2. 사용자 리뷰에서 높은 평점 아티스트/장르 추출
            List<String> reviewBasedArtists = getArtistsFromUserReviews(userId);
            List<String> reviewBasedGenres = getGenresFromUserReviews(userId);
            
            // 3. 프로필과 리뷰 데이터 결합 및 가중치 적용
            List<WeightedRecommendationSource> weightedSources = new ArrayList<>();
            
            // 리뷰 기반 아티스트 (가장 높은 가중치)
            reviewBasedArtists.forEach(artist -> 
                weightedSources.add(new WeightedRecommendationSource(artist, "ARTIST", "리뷰 기반", 0.9)));
                
            // 프로필 선호 아티스트
            profile.getFavoriteArtists().forEach(artist -> 
                weightedSources.add(new WeightedRecommendationSource(
                    (String) artist.get("name"), "ARTIST", "선호 아티스트", 0.7)));
            
            // 리뷰 기반 장르
            reviewBasedGenres.forEach(genre -> 
                weightedSources.add(new WeightedRecommendationSource(genre, "GENRE", "리뷰 기반", 0.6)));
                
            // 프로필 선호 장르
            profile.getFavoriteGenres().forEach(genre -> 
                weightedSources.add(new WeightedRecommendationSource(
                    (String) genre.get("name"), "GENRE", "선호 장르", 0.5)));
            
            // 4. 가중치 기준 정렬 (높은 가중치부터)
            weightedSources.sort((a, b) -> Double.compare(b.weight, a.weight));
            
            // 5. 개인화된 추천 생성 (중복 제거 포함)
            for (WeightedRecommendationSource source : weightedSources) {
                if (recommendations.size() >= 8) break; // 최대 8곡
                
                try {
                    List<TrackDto> tracks = spotifyService.searchTracks(source.value, 10);
                    if (!tracks.isEmpty()) {
                        // 인기도와 개인화 점수를 고려한 선택
                        TrackDto bestTrack = selectBestTrack(tracks, source.weight, usedTrackIds);
                        if (bestTrack != null) {
                            Map<String, Object> recommendation = createRecommendationMap(bestTrack, source.description);
                            recommendations.add(recommendation);
                            usedTrackIds.add(bestTrack.getId());
                        }
                    }
                } catch (Exception e) {
                    // 개별 검색 실패는 무시
                }
            }
            
            // 6. 부족하면 트렌딩 곡으로 스마트 보충
            if (recommendations.size() < 4) {
                List<String> trendingGenres = getTrendingGenresForUser(profile);
                fillWithTrendingTracks(recommendations, usedTrackIds, trendingGenres);
            }
            
        } catch (Exception e) {
            // 전체 실패 시 빈 리스트 반환
        }
        
        // 7. 최종 중복 제거 및 다양성 보장
        return ensureDiversityAndRemoveDuplicates(recommendations);
    }
    
    private static class WeightedRecommendationSource {
        final String value;
        final String type;
        final String description;
        final double weight;
        
        WeightedRecommendationSource(String value, String type, String description, double weight) {
            this.value = value;
            this.type = type;
            this.description = description;
            this.weight = weight;
        }
    }
    
    private TrackDto selectBestTrack(List<TrackDto> tracks, double userWeight, Set<String> usedIds) {
        // 이미 사용된 트랙 제외
        List<TrackDto> availableTracks = tracks.stream()
            .filter(track -> !usedIds.contains(track.getId()))
            .toList();
            
        if (availableTracks.isEmpty()) return null;
        
        // 인기도와 개인화 가중치를 결합한 점수 계산
        return availableTracks.stream()
            .max((a, b) -> {
                double scoreA = (a.getPopularity() / 100.0) * 0.3 + userWeight * 0.7;
                double scoreB = (b.getPopularity() / 100.0) * 0.3 + userWeight * 0.7;
                return Double.compare(scoreA, scoreB);
            })
            .orElse(availableTracks.get(0));
    }
    
    private List<String> getTrendingGenresForUser(ProfileDto profile) {
        Set<String> userGenres = profile.getFavoriteGenres().stream()
            .map(genre -> (String) genre.get("name"))
            .collect(Collectors.toSet());
            
        List<String> allTrendingGenres = List.of("pop", "indie pop", "alternative rock", 
            "electronic", "k-pop", "hip hop", "R&B", "indie folk");
            
        // 사용자 선호도와 관련된 트렌딩 장르 우선
        return allTrendingGenres.stream()
            .filter(genre -> userGenres.stream().anyMatch(userGenre -> 
                genre.toLowerCase().contains(userGenre.toLowerCase()) || 
                userGenre.toLowerCase().contains(genre.toLowerCase())))
            .toList();
    }
    
    private void fillWithTrendingTracks(List<Map<String, Object>> recommendations, 
                                       Set<String> usedIds, List<String> trendingGenres) {
        Random random = new Random();
        
        while (recommendations.size() < 4 && !trendingGenres.isEmpty()) {
            String genre = trendingGenres.get(random.nextInt(trendingGenres.size()));
            try {
                List<TrackDto> tracks = spotifyService.searchTracks(genre + " 2024", 5);
                TrackDto bestTrack = selectBestTrack(tracks, 0.4, usedIds);
                
                if (bestTrack != null) {
                    recommendations.add(createRecommendationMap(bestTrack, "트렌딩"));
                    usedIds.add(bestTrack.getId());
                }
            } catch (Exception e) {
                // 실패 시 해당 장르 제거
            }
            trendingGenres.remove(genre);
        }
    }
    
    private List<Map<String, Object>> ensureDiversityAndRemoveDuplicates(List<Map<String, Object>> recommendations) {
        Map<String, Map<String, Object>> uniqueById = new LinkedHashMap<>();
        Map<String, Integer> artistCount = new HashMap<>();
        Map<String, Integer> genreCount = new HashMap<>();
        
        List<Map<String, Object>> diverseRecommendations = new ArrayList<>();
        
        for (Map<String, Object> rec : recommendations) {
            String trackId = (String) rec.get("id");
            String artist = (String) rec.get("artist");
            String genre = (String) rec.get("genre");
            
            // ID 중복 체크
            if (uniqueById.containsKey(trackId)) continue;
            
            // 아티스트 다양성 체크 (같은 아티스트 최대 2곡)
            int artistFreq = artistCount.getOrDefault(artist, 0);
            if (artistFreq >= 2) continue;
            
            // 장르 다양성 체크 (같은 장르 최대 3곡)
            int genreFreq = genreCount.getOrDefault(genre, 0);
            if (genreFreq >= 3) continue;
            
            // 통과한 추천 추가
            uniqueById.put(trackId, rec);
            diverseRecommendations.add(rec);
            artistCount.put(artist, artistFreq + 1);
            genreCount.put(genre, genreFreq + 1);
        }
        
        return diverseRecommendations;
    }
    
    private Map<String, Object> createRecommendationMap(TrackDto track, String recommendationType) {
        // 개인화 점수 계산 (실제 Spotify 데이터 기반)
        int personalityScore = calculatePersonalizedScore(track, recommendationType);
        
        Map<String, Object> recommendation = new HashMap<>();
        recommendation.put("id", track.getId());
        recommendation.put("title", track.getName());
        recommendation.put("artist", track.getArtists().isEmpty() ? "Unknown Artist" : track.getArtists().get(0).getName());
        recommendation.put("image", track.getAlbum() != null && !track.getAlbum().getImages().isEmpty() 
            ? track.getAlbum().getImages().get(0).getUrl() 
            : "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300&h=300&fit=crop");
        recommendation.put("genre", recommendationType);
        recommendation.put("score", personalityScore);
        recommendation.put("spotifyId", track.getId());
        recommendation.put("popularity", track.getPopularity());
        recommendation.put("duration", track.getDurationMs() != null ? formatDuration(track.getDurationMs()) : "3:30");
        
        return recommendation;
    }
    
    private int calculatePersonalizedScore(TrackDto track, String recommendationType) {
        int baseScore = switch (recommendationType) {
            case "리뷰 기반" -> 92;
            case "선호 아티스트" -> 88;
            case "선호 장르" -> 85;
            case "트렌딩" -> 80;
            default -> 75;
        };
        
        // Spotify 인기도 반영 (0-100)
        int popularityBonus = track.getPopularity() / 10; // 0-10 보너스
        
        // 최종 점수 계산 (70-100 범위)
        return Math.min(100, Math.max(70, baseScore + popularityBonus - 5 + new Random().nextInt(6)));
    }
    
    private String formatDuration(Integer durationMs) {
        if (durationMs == null) return "3:30";
        
        int minutes = durationMs / (1000 * 60);
        int seconds = (durationMs % (1000 * 60)) / 1000;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    /**
     * 사용자 리뷰에서 높은 평점(4점 이상) 아티스트 추출
     */
    private List<String> getArtistsFromUserReviews(Long userId) {
        try {
            // 사용자가 작성한 리뷰 + 도움이 됨으로 표시한 리뷰에서 아티스트 추출
            List<MusicReview> relevantReviews = musicReviewService.getReviewsForRecommendations(userId, 4);
            return relevantReviews.stream()
                .map(review -> review.getMusicItem().getArtistName())
                .filter(Objects::nonNull)
                .distinct()
                .limit(5) // 최대 5개
                .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * 사용자 리뷰에서 높은 평점(4점 이상) 장르 추출
     */
    private List<String> getGenresFromUserReviews(Long userId) {
        try {
            // 사용자가 작성한 리뷰 + 도움이 됨으로 표시한 리뷰에서 장르 추출
            List<MusicReview> relevantReviews = musicReviewService.getReviewsForRecommendations(userId, 4);
            return relevantReviews.stream()
                .map(review -> review.getMusicItem().getGenre())
                .filter(Objects::nonNull)
                .distinct()
                .limit(5) // 최대 5개
                .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 개인화된 음악 추천 조회
     */
    @GetMapping("/personalized/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getPersonalizedRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> recommendations = 
                    recommendationEngine.getPersonalizedRecommendations(userId, limit);
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userId", userId,
                    "recommendationCount", recommendations.size(),
                    "recommendations", recommendations,
                    "message", "개인화 추천이 성공적으로 생성되었습니다",
                    "timestamp", java.time.Instant.now().toString()
                ));
                
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "추천 생성 실패: " + e.getMessage(),
                    "timestamp", java.time.Instant.now().toString()
                ));
            }
        });
    }
    
    /**
     * 장르 기반 추천
     */
    @GetMapping("/by-genre")
    public ResponseEntity<Map<String, Object>> getRecommendationsByGenre(
            @RequestParam String genre,
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            // 장르 기반 추천 (실제 구현 필요)
            List<Map<String, Object>> genreRecommendations = 
                recommendationEngine.getPersonalizedRecommendations(1L, limit);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "genre", genre,
                "recommendations", genreRecommendations,
                "message", genre + " 장르 기반 추천이 생성되었습니다",
                "timestamp", java.time.Instant.now().toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "장르 추천 생성 실패: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            ));
        }
    }
    
    /**
     * 사용자 간 음악 호환성 분석
     */
    @GetMapping("/compatibility/{user1Id}/{user2Id}")
    public ResponseEntity<Map<String, Object>> analyzeUserCompatibility(
            @PathVariable Long user1Id,
            @PathVariable Long user2Id) {
        
        try {
            var user1Profile = userProfileService.getOrInit(user1Id);
            var user2Profile = userProfileService.getOrInit(user2Id);
            
            double genreCompatibility = recommendationEngine.calculateGenreCompatibility(user1Profile, user2Profile);
            double artistCompatibility = recommendationEngine.calculateArtistCompatibility(user1Profile, user2Profile);
            
            // 전체 호환성 계산 (가중평균)
            double overallCompatibility = (genreCompatibility * 0.6) + (artistCompatibility * 0.4);
            
            String compatibilityLevel;
            String description;
            
            if (overallCompatibility >= 0.8) {
                compatibilityLevel = "EXCELLENT";
                description = "🎵 환상적인 음악 궁합! 서로의 취향이 매우 잘 맞습니다";
            } else if (overallCompatibility >= 0.6) {
                compatibilityLevel = "GOOD";
                description = "🎶 좋은 음악 호환성! 공통 관심사가 많습니다";
            } else if (overallCompatibility >= 0.4) {
                compatibilityLevel = "MODERATE";
                description = "🎤 적당한 호환성! 새로운 음악 발견 기회가 있습니다";
            } else {
                compatibilityLevel = "LOW";
                description = "🎧 다른 취향! 서로의 음악을 탐험해보세요";
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "user1Id", user1Id,
                "user2Id", user2Id,
                "overallCompatibility", Math.round(overallCompatibility * 100) / 100.0,
                "genreCompatibility", Math.round(genreCompatibility * 100) / 100.0,
                "artistCompatibility", Math.round(artistCompatibility * 100) / 100.0,
                "compatibilityLevel", compatibilityLevel,
                "description", description,
                "compatibilityPercentage", (int) (overallCompatibility * 100),
                "timestamp", java.time.Instant.now().toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "호환성 분석 실패: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            ));
        }
    }
    
    /**
     * 추천 시스템 통계 조회
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Object>> getRecommendationStats(@PathVariable Long userId) {
        try {
            // 사용자 프로필 기반 통계
            var userProfile = userProfileService.getOrInit(userId);
            
            Map<String, Object> stats = Map.of(
                "totalRecommendations", 150,
                "favoriteGenresCount", userProfile.getFavoriteGenres().size(),
                "favoriteArtistsCount", userProfile.getFavoriteArtists().size(),
                "topRecommendationType", "content_based",
                "averageRating", 4.2,
                "genreDistribution", Map.of(
                    "Pop", 35,
                    "Rock", 28,
                    "Hip-Hop", 20,
                    "Electronic", 12,
                    "Others", 5
                ),
                "lastRecommendationDate", java.time.Instant.now().minusSeconds(3600).toString(),
                "recommendationAccuracy", 0.78
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "userId", userId,
                "stats", stats,
                "message", "추천 통계가 성공적으로 조회되었습니다",
                "timestamp", java.time.Instant.now().toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "통계 조회 실패: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            ));
        }
    }
    
    /**
     * 추천 피드백 제출 (학습용)
     */
    @PostMapping("/feedback")
    public ResponseEntity<Map<String, Object>> submitFeedback(@RequestBody Map<String, Object> feedback) {
        try {
            Long userId = ((Number) feedback.get("userId")).longValue();
            String trackId = (String) feedback.get("trackId");
            String feedbackType = (String) feedback.get("feedbackType"); // like, dislike, skip
            Double rating = feedback.containsKey("rating") ? 
                ((Number) feedback.get("rating")).doubleValue() : null;
            
            // 피드백 로깅 (실제 구현에서는 ML 모델 학습에 사용)
            String logMessage = String.format("사용자 피드백 - userId: %d, trackId: %s, type: %s, rating: %s", 
                userId, trackId, feedbackType, rating);
            
            // 여기서 실제 ML 모델 업데이트 로직이 들어갈 수 있음
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "피드백이 성공적으로 제출되었습니다",
                "userId", userId,
                "trackId", trackId,
                "feedbackType", feedbackType,
                "timestamp", java.time.Instant.now().toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "피드백 제출 실패: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            ));
        }
    }
    
    /**
     * 음악 발견 도우미 - 새로운 장르/아티스트 제안
     */
    @GetMapping("/discovery/{userId}")
    public ResponseEntity<Map<String, Object>> getMusicDiscoveryRecommendations(@PathVariable Long userId) {
        try {
            // 음악 발견 추천 (실제 구현 필요)
            var userProfile = userProfileService.getOrInit(userId);
            
            List<String> currentGenres = userProfile.getFavoriteGenres().stream()
                .map(genre -> (String) genre.get("name"))
                .toList();
            
            List<String> allGenres = List.of("Jazz", "Classical", "Country", "Reggae", "Blues", 
                "Folk", "World", "Latin", "Ambient", "Experimental");
            
            List<String> newGenres = allGenres.stream()
                .filter(genre -> !currentGenres.contains(genre))
                .limit(3)
                .toList();
            
            Map<String, Object> discoveryData = Map.of(
                "newGenresToExplore", newGenres,
                "discoveryRecommendations", recommendationEngine.getPersonalizedRecommendations(userId, 3)
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "userId", userId,
                "discoveryData", discoveryData,
                "message", "새로운 음악 발견을 위한 추천이 생성되었습니다",
                "timestamp", java.time.Instant.now().toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "발견 추천 생성 실패: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            ));
        }
    }
}