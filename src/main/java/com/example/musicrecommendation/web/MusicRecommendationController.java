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
        Random random = new Random();
        
        try {
            // 1. 사용자 프로필 가져오기
            ProfileDto profile = userProfileService.getOrInit(userId);
            
            // 2. 사용자 리뷰에서 높은 평점 아티스트/장르 추출
            List<String> reviewBasedArtists = getArtistsFromUserReviews(userId);
            List<String> reviewBasedGenres = getGenresFromUserReviews(userId);
            
            // 3. 프로필과 리뷰 데이터 결합
            List<String> allArtists = new ArrayList<>();
            List<String> allGenres = new ArrayList<>();
            
            // 프로필 선호 아티스트 추가
            List<Map<String, Object>> favoriteArtists = profile.getFavoriteArtists();
            favoriteArtists.forEach(artist -> allArtists.add((String) artist.get("name")));
            
            // 리뷰 기반 아티스트 추가
            allArtists.addAll(reviewBasedArtists);
            
            // 프로필 선호 장르 추가
            List<Map<String, Object>> favoriteGenres = profile.getFavoriteGenres();
            favoriteGenres.forEach(genre -> allGenres.add((String) genre.get("name")));
            
            // 리뷰 기반 장르 추가
            allGenres.addAll(reviewBasedGenres);
            
            // 4. 랜덤하게 섞어서 다양성 확보
            Collections.shuffle(allArtists, random);
            Collections.shuffle(allGenres, random);
            
            // 5. 아티스트 기반 추천 (최대 2곡)
            for (int i = 0; i < Math.min(2, allArtists.size()) && recommendations.size() < 4; i++) {
                String artistName = allArtists.get(i);
                try {
                    List<TrackDto> tracks = spotifyService.searchTracks(artistName, 5); // 더 많이 검색
                    if (!tracks.isEmpty()) {
                        // 랜덤하게 선택
                        TrackDto selectedTrack = tracks.get(random.nextInt(tracks.size()));
                        String source = reviewBasedArtists.contains(artistName) ? "리뷰 기반" : "선호 아티스트";
                        recommendations.add(createRecommendationMap(selectedTrack, source));
                    }
                } catch (Exception e) {
                    // 개별 검색 실패는 무시
                }
            }
            
            // 6. 장르 기반 추천 (나머지 채우기)
            for (int i = 0; i < allGenres.size() && recommendations.size() < 4; i++) {
                String genreName = allGenres.get(i);
                try {
                    List<TrackDto> tracks = spotifyService.searchTracks(genreName, 5); // 더 많이 검색
                    if (!tracks.isEmpty()) {
                        // 랜덤하게 선택
                        TrackDto selectedTrack = tracks.get(random.nextInt(tracks.size()));
                        String source = reviewBasedGenres.contains(genreName) ? "리뷰 기반" : "선호 장르";
                        recommendations.add(createRecommendationMap(selectedTrack, source));
                    }
                } catch (Exception e) {
                    // 개별 검색 실패는 무시
                }
            }
            
            // 7. 부족하면 인기곡으로 채우기
            if (recommendations.size() < 4) {
                try {
                    List<String> fallbackGenres = List.of("pop", "rock", "indie", "electronic", "hip-hop");
                    String randomGenre = fallbackGenres.get(random.nextInt(fallbackGenres.size()));
                    List<TrackDto> tracks = spotifyService.searchTracks(randomGenre, 10);
                    
                    while (recommendations.size() < 4 && !tracks.isEmpty()) {
                        TrackDto randomTrack = tracks.get(random.nextInt(tracks.size()));
                        recommendations.add(createRecommendationMap(randomTrack, "인기곡"));
                        tracks.remove(randomTrack); // 중복 방지
                    }
                } catch (Exception e) {
                    // 기본 검색도 실패하면 빈 리스트 반환
                }
            }
            
        } catch (Exception e) {
            // 전체 실패 시 빈 리스트 반환
        }
        
        return recommendations;
    }
    
    private Map<String, Object> createRecommendationMap(TrackDto track, String recommendationType) {
        return Map.of(
                "id", track.getId(),
                "title", track.getName(),
                "artist", track.getArtists().isEmpty() ? "Unknown Artist" : track.getArtists().get(0).getName(),
                "image", track.getAlbum() != null && !track.getAlbum().getImages().isEmpty() 
                    ? track.getAlbum().getImages().get(0).getUrl() 
                    : "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300&h=300&fit=crop",
                "genre", recommendationType,
                "score", 85 + new Random().nextInt(15)
        );
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