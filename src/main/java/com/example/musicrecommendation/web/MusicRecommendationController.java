package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.MusicRecommendationEngine;
import com.example.musicrecommendation.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MusicRecommendationController {
    
    private final MusicRecommendationEngine recommendationEngine;
    private final UserProfileService userProfileService;
    
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
            // 시뮬레이션 데이터 생성
            List<Map<String, Object>> genreRecommendations = List.of(
                Map.of(
                    "id", "genre_rec_1",
                    "name", "Best " + genre + " Track",
                    "artist", genre + " Artist #1",
                    "album", "Greatest " + genre + " Hits",
                    "genre", genre,
                    "duration", "3:45",
                    "popularity", 88,
                    "recommendationType", "genre_based"
                ),
                Map.of(
                    "id", "genre_rec_2",
                    "name", "Popular " + genre + " Song",
                    "artist", genre + " Artist #2", 
                    "album", "Top " + genre + " Collection",
                    "genre", genre,
                    "duration", "4:12",
                    "popularity", 82,
                    "recommendationType", "genre_based"
                )
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "genre", genre,
                "recommendations", genreRecommendations.stream().limit(limit).toList(),
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
            // 사용자 프로필 기반 통계 생성
            var userProfile = userProfileService.getOrInit(userId);
            
            // 기본 통계 (시뮬레이션)
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
            var userProfile = userProfileService.getOrInit(userId);
            
            // 사용자가 아직 탐험하지 않은 장르 제안
            List<String> currentGenres = userProfile.getFavoriteGenres().stream()
                .map(genre -> (String) genre.get("name"))
                .toList();
            
            List<String> allGenres = List.of("Jazz", "Classical", "Country", "Reggae", "Blues", 
                "Folk", "World", "Latin", "Ambient", "Experimental");
            
            List<String> newGenres = allGenres.stream()
                .filter(genre -> !currentGenres.contains(genre))
                .limit(3)
                .toList();
            
            // 발견 추천 생성
            List<Map<String, Object>> discoveryTracks = new ArrayList<>();
            for (String genre : newGenres) {
                discoveryTracks.add(Map.of(
                    "id", "discovery_" + genre.toLowerCase(),
                    "name", "Discover " + genre,
                    "artist", genre + " Gateway Artist",
                    "album", "Introduction to " + genre,
                    "genre", genre,
                    "duration", "4:00",
                    "popularity", 70,
                    "discoveryReason", genre + " 장르를 탐험해보세요!"
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "userId", userId,
                "newGenresToExplore", newGenres,
                "discoveryRecommendations", discoveryTracks,
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