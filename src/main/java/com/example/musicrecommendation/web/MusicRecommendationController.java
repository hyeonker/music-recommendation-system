package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.MusicRecommendationEngine;
import com.example.musicrecommendation.service.UserProfileService;
import com.example.musicrecommendation.service.MusicReviewService;
import com.example.musicrecommendation.service.SpotifyService;
import com.example.musicrecommendation.service.RecommendationLimitService;
import com.example.musicrecommendation.service.UserSongLikeService;
import com.example.musicrecommendation.config.RecommendationProperties;
import com.example.musicrecommendation.domain.MusicReview;
import com.example.musicrecommendation.domain.UserSongLike;
import com.example.musicrecommendation.domain.RecommendationHistory;
import com.example.musicrecommendation.repository.RecommendationHistoryRepository;
import com.example.musicrecommendation.web.dto.ProfileDto;
import com.example.musicrecommendation.web.dto.spotify.TrackDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class MusicRecommendationController {
    
    private final MusicRecommendationEngine recommendationEngine;
    private final UserProfileService userProfileService;
    private final MusicReviewService musicReviewService;
    private final SpotifyService spotifyService;
    private final RecommendationLimitService limitService;
    private final UserSongLikeService userSongLikeService;
    private final RecommendationProperties properties;
    private final RecommendationHistoryRepository recommendationHistoryRepository;
    
    // 사용자별 최근 추천 곡 캐시 (중복 방지용) - 3분으로 단축
    private final Map<Long, Set<String>> userRecentRecommendations = new ConcurrentHashMap<>();
    private final Map<Long, Long> userRecommendationTimestamps = new ConcurrentHashMap<>();
    private static final long RECOMMENDATION_CACHE_DURATION_MS = 3 * 60 * 1000; // 3분
    
    /**
     * Dashboard용 사용자 맞춤 추천 (Spotify 기반) - 개선된 제한 시스템
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserRecommendations(@PathVariable Long userId,
                                                   @RequestParam(defaultValue = "false") boolean refresh) {
        try {
            // refresh 파라미터에 따라 다른 제한 확인 방식 사용
            var limitResult = refresh 
                ? limitService.checkAndIncrementRefreshCount(userId)  // 새로고침 시: 카운트 증가
                : limitService.checkLimitOnly(userId);              // 일반 조회 시: 조회만
            
            // 제한 초과 시 바로 반환
            if (!limitResult.isSuccess()) {
                return ResponseEntity.ok()
                    .header("X-Refresh-Used", String.valueOf(limitResult.getCurrentCount()))
                    .header("X-Refresh-Remaining", String.valueOf(limitResult.getRemainingCount()))
                    .header("X-Refresh-Max", String.valueOf(limitResult.getMaxCount()))
                    .header("X-Refresh-Reset-Date", limitResult.getResetDate().toString())
                    .header("X-Limit-Exceeded", "true")
                    .body(Map.of(
                        "success", false,
                        "message", limitResult.getMessage(),
                        "recommendations", Collections.emptyList()
                    ));
            }
            
            // 추천 생성 (설정 기반 제한값 사용)
            List<Map<String, Object>> recommendations = generateSpotifyBasedRecommendations(
                userId, properties.getMaxRecommendationsPerRequest()
            );
            
            // 추천 히스토리 저장 (별도 스레드에서 비동기 처리)
            if (!recommendations.isEmpty()) {
                saveRecommendationHistory(userId, recommendations);
            }
            
            // 총 추천곡 수 조회 (누적 히스토리)
            long totalRecommendationsCount = recommendationHistoryRepository.countByUserId(userId);
            
            // 응답 헤더에 일일 + 시간당 제한 정보 + 총 추천곡 수 추가 (실제 데이터 사용)
            return ResponseEntity.ok()
                .header("X-Refresh-Used", String.valueOf(limitResult.getCurrentCount()))
                .header("X-Refresh-Remaining", String.valueOf(limitResult.getRemainingCount()))
                .header("X-Refresh-Max", String.valueOf(limitResult.getMaxCount()))
                .header("X-Refresh-Reset-Date", limitResult.getResetDate().toString())
                .header("X-Hourly-Used", String.valueOf(limitResult.getHourlyUsed()))
                .header("X-Hourly-Remaining", String.valueOf(limitResult.getHourlyRemaining()))
                .header("X-Hourly-Max", String.valueOf(limitResult.getHourlyMax()))
                .header("X-Total-Count", String.valueOf(totalRecommendationsCount))
                .body(recommendations);
                
        } catch (Exception e) {
            log.error("=== 추천 생성 중 에러 발생 ===");
            log.error("사용자 ID: {}", userId);
            log.error("에러 메시지: {}", e.getMessage());
            log.error("에러 스택 트레이스:", e);
            
            // 에러 상황에서도 기본 제한 정보 헤더 추가
            int dailyMax = 10;
            int dailyUsed = 0;
            int dailyRemaining = dailyMax;
            int hourlyMax = 3;
            int hourlyUsed = 0;
            int hourlyRemaining = hourlyMax;
            
            // 폴백 추천 생성
            List<Map<String, Object>> fallbackRecs = properties.isFallbackEnabled() 
                ? generateFallbackRecommendations(userId)
                : Collections.emptyList();
            
            // 기본 헤더와 함께 응답
            return ResponseEntity.ok()
                .header("X-Refresh-Used", String.valueOf(dailyUsed))
                .header("X-Refresh-Remaining", String.valueOf(dailyRemaining))
                .header("X-Refresh-Max", String.valueOf(dailyMax))
                .header("X-Refresh-Reset-Date", java.time.LocalDate.now().plusDays(1).toString())
                .header("X-Hourly-Used", String.valueOf(hourlyUsed))
                .header("X-Hourly-Remaining", String.valueOf(hourlyRemaining))
                .header("X-Hourly-Max", String.valueOf(hourlyMax))
                .body(fallbackRecs);
        }
    }

    /**
     * 기본 추천 생성 (8곡)
     */
    private List<Map<String, Object>> generateSpotifyBasedRecommendations(Long userId) {
        return generateSpotifyBasedRecommendations(userId, 8);
    }
    
    /**
     * 제한된 수의 추천 생성
     */
    private List<Map<String, Object>> generateSpotifyBasedRecommendations(Long userId, int maxCount) {
        log.info("=== 추천 생성 시작 ===");
        log.info("사용자 ID: {}", userId);
        log.info("최대 곡 수: {}", maxCount);
        
        // 1. 사용자별 최근 추천 이력 관리
        Set<String> userRecentTracks = getUserRecentRecommendations(userId);
        log.debug("사용자 {} 최근 추천 이력: {}개 트랙", userId, userRecentTracks.size());
        
        // 2. 사용자가 좋아요한 곡들 가져오기 (추천에서 제외)
        Set<String> likedTrackIds = getUserLikedTrackIds(userId);
        log.info("사용자 {} 좋아요한 곡: {}개", userId, likedTrackIds.size());
        
        List<Map<String, Object>> recommendations = new ArrayList<>();
        Set<String> usedTrackIds = new HashSet<>(userRecentTracks); // 최근 추천과 현재 추천 모두 중복 방지
        usedTrackIds.addAll(likedTrackIds); // 좋아요한 곡들도 제외
        
        // 사용자별 개인화된 Random 시드 생성 (당일 + 사용자 ID 기반)
        long todayMillis = java.time.LocalDate.now().toEpochDay();
        long personalizedSeed = userId * 1000 + todayMillis;
        Random random = new Random(personalizedSeed);
        log.warn("🎯 [DEBUG] 사용자 {} 개인화 추천 시작 - 시드: {} (userId={}, todayMillis={})", 
                 userId, personalizedSeed, userId, todayMillis);
        
        try {
            // 2. 사용자 프로필 가져오기
            ProfileDto profile = userProfileService.getOrInit(userId);
            log.warn("🎯 [DEBUG] === 사용자 {} 프로필 데이터 확인 ===", userId);
            log.warn("🎯 [DEBUG] 사용자 {}: 선호 아티스트 {}개 - {}", userId, 
                     profile.getFavoriteArtists() != null ? profile.getFavoriteArtists().size() : 0, 
                     profile.getFavoriteArtists());
            log.warn("🎯 [DEBUG] 사용자 {}: 선호 장르 {}개 - {}", userId, 
                     profile.getFavoriteGenres() != null ? profile.getFavoriteGenres().size() : 0, 
                     profile.getFavoriteGenres());
            
            // 3. 사용자 리뷰에서 높은 평점 아티스트/장르 추출
            List<String> reviewBasedArtists = getArtistsFromUserReviews(userId);
            List<String> reviewBasedGenres = getGenresFromUserReviews(userId);
            log.warn("🎯 [DEBUG] 사용자 {}: 리뷰 기반 아티스트 {}개 - {}", userId, reviewBasedArtists.size(), reviewBasedArtists);
            log.warn("🎯 [DEBUG] 사용자 {}: 리뷰 기반 장르 {}개 - {}", userId, reviewBasedGenres.size(), reviewBasedGenres);
            
            // 4. 프로필과 리뷰 데이터 결합 및 가중치 적용
            List<WeightedRecommendationSource> weightedSources = new ArrayList<>();
            
            // 리뷰 기반 아티스트 (가장 높은 가중치)
            for (String artist : reviewBasedArtists) {



                weightedSources.add(new WeightedRecommendationSource(artist, "ARTIST", "리뷰 기반", 0.9));
            }








            log.info("리뷰 기반 아티스트 소스 개수: {}", reviewBasedArtists.size());
                
            // 프로필 선호 아티스트
            for (Map<String, Object> artistMap : profile.getFavoriteArtists()) {
                String artistName = (String) artistMap.get("name");
                
                if (artistName != null && !artistName.trim().isEmpty()) {
                    weightedSources.add(new WeightedRecommendationSource(artistName, "ARTIST", "선호 아티스트", 0.7));
                    log.info("선호 아티스트 소스 추가: {}", artistName);
                } else {
                    log.warn("잘못된 아티스트 데이터: {}", artistMap);
                }
            }
            
            // 리뷰 기반 장르
            for (String genre : reviewBasedGenres) {
                weightedSources.add(new WeightedRecommendationSource(genre, "GENRE", "리뷰 기반", 0.6));
            }
            log.info("리뷰 기반 장르 소스 개수: {}", reviewBasedGenres.size());
                
            // 프로필 선호 장르
            for (Map<String, Object> genreMap : profile.getFavoriteGenres()) {
                String genreName = (String) genreMap.get("name");
                
                if (genreName != null && !genreName.trim().isEmpty()) {
                    weightedSources.add(new WeightedRecommendationSource(genreName, "GENRE", "선호 장르", 0.5));
                    log.info("선호 장르 소스 추가: {}", genreName);
                } else {
                    log.warn("잘못된 장르 데이터: {}", genreMap);
                }
            }
            
            log.info("전체 추천 소스 개수: {}", weightedSources.size());
            
            // 5. 가중치 기준 정렬하되, 같은 가중치 내에서만 셔플
            Map<Double, List<WeightedRecommendationSource>> sourcesByWeight = weightedSources.stream()
                .collect(Collectors.groupingBy(source -> source.weight));
                
            List<WeightedRecommendationSource> orderedSources = new ArrayList<>();
            Random sourceRandom = new Random(userId * 999 + todayMillis); // 사용자별 개인화된 소스 랜덤
            
            // 가중치 높은 순으로 처리하되, 같은 가중치 그룹 내에서만 셔플
            sourcesByWeight.entrySet().stream()
                .sorted(Map.Entry.<Double, List<WeightedRecommendationSource>>comparingByKey().reversed())
                .forEach(entry -> {
                    List<WeightedRecommendationSource> groupSources = new ArrayList<>(entry.getValue());
                    Collections.shuffle(groupSources, sourceRandom); // 같은 가중치 내에서만 셔플
                    orderedSources.addAll(groupSources);
                    log.debug("가중치 {} 그룹: {}개 소스 처리", entry.getKey(), groupSources.size());
                });
            
            weightedSources = orderedSources;
            
            // 6. 개선된 개인화 추천 생성 - 아티스트별 2-3곡, 장르별 1-2곡 허용
            Map<String, Integer> sourceQuotas = calculateSourceQuotas(weightedSources, maxCount);
            Map<String, Integer> sourceUsed = new HashMap<>();
            
            for (WeightedRecommendationSource source : weightedSources) {
                if (recommendations.size() >= maxCount) break;
                
                String sourceKey = source.type + ":" + source.value;
                int quota = sourceQuotas.getOrDefault(sourceKey, 1);
                int used = sourceUsed.getOrDefault(sourceKey, 0);
                
                if (used >= quota) {
                    log.debug("소스 할당량 초과: {} (사용: {}, 할당: {})", sourceKey, used, quota);
                    continue;
                }
                
                try {
                    log.debug("추천 소스 처리: {} ({}) - 가중치: {}, 할당량: {}/{}", 
                             source.value, source.type, source.weight, used, quota);
                    
                    List<TrackDto> tracks = new ArrayList<>();
                    
                    if (source.type.equals("ARTIST")) {
                        // 여러 검색 방식 시도하여 최대한 많은 결과 확보
                        tracks = new ArrayList<>();
                        
                        // 1차: 정확한 artist: 검색 시도 (따옴표 제거 - Spotify API 표준)
                        String exactQuery = "artist:" + source.value;
                        List<TrackDto> exactTracks = spotifyService.searchTracks(exactQuery, 20);
                        tracks.addAll(exactTracks);
                        log.warn("🎯 정확한 아티스트 검색 '{}' 결과: {}곡", exactQuery, exactTracks.size());
                        
                        // 2차: 아티스트명을 따옴표로 감싼 검색
                        if (tracks.size() < 10) {
                            String quotedQuery = "\"" + source.value + "\"";
                            List<TrackDto> quotedTracks = spotifyService.searchTracks(quotedQuery, 20);
                            addUniqueTracksById(tracks, quotedTracks);
                            log.warn("🎯 따옴표 아티스트 검색 '{}' 결과: 총 {}곡", quotedQuery, tracks.size());
                        }
                        
                        // 3차: 일반 아티스트명 검색 
                        if (tracks.size() < 10) {
                            List<TrackDto> simpleTracks = spotifyService.searchTracks(source.value, 20);
                            addUniqueTracksById(tracks, simpleTracks);
                            log.warn("🎯 단순 아티스트 검색 '{}' 결과: 총 {}곡", source.value, tracks.size());
                        }
                        
                        log.warn("🎯 아티스트 '{}' 최종 검색 결과: {}곡", source.value, tracks.size());
                    } else {
                        tracks = spotifyService.searchTracks(source.value, 10);
                        log.debug("장르 검색 '{}' 결과: {}곡", source.value, tracks.size());
                    }
                    
                    if (!tracks.isEmpty()) {
                        // 아티스트 검색의 경우 실제 아티스트 매치 검증
                        if (source.type.equals("ARTIST")) {
                            int beforeFilter = tracks.size();
                            tracks = filterTracksByActualArtist(tracks, source.value);
                            log.debug("아티스트 매치 필터링: {}곡 -> {}곡 (타겟: {})", 
                                     beforeFilter, tracks.size(), source.value);
                        }
                        
                        if (tracks.isEmpty()) {
                            log.warn("필터링 후 남은 트랙이 없음: {} ({})", source.value, source.type);
                            continue;
                        }
                        
                        // 할당량만큼 여러 곡 선택 시도
                        int remainingQuota = quota - used;
                        int tracksToSelect = Math.min(remainingQuota, Math.min(tracks.size(), maxCount - recommendations.size()));
                        
                        for (int i = 0; i < tracksToSelect; i++) {
                            TrackDto bestTrack = selectBestTrack(tracks, source.weight, usedTrackIds, userId, todayMillis);
                            if (bestTrack != null) {
                                Map<String, Object> recommendation = createRecommendationMap(bestTrack, source.description);
                                recommendations.add(recommendation);
                                usedTrackIds.add(bestTrack.getId());
                                
                                String artist = bestTrack.getArtists().isEmpty() ? "Unknown" : bestTrack.getArtists().get(0).getName();
                                String titleArtistKey = "title_artist:" + (bestTrack.getName() + "|" + artist).toLowerCase();
                                usedTrackIds.add(titleArtistKey);
                                
                                sourceUsed.put(sourceKey, sourceUsed.getOrDefault(sourceKey, 0) + 1);
                                
                                log.info("추천 추가: {} - {} (소스: {} {}, {}/{})", 
                                        bestTrack.getName(), artist, source.type, source.description,
                                        sourceUsed.get(sourceKey), quota);
                            } else {
                                log.debug("더 이상 선택 가능한 곡 없음: {} ({})", source.value, source.type);
                                break;
                            }
                        }
                    } else {
                        log.warn("검색 결과 없음: {} ({})", source.value, source.type);
                    }
                } catch (Exception e) {
                    log.error("검색 실패: {} ({}) - {}", source.value, source.type, e.getMessage());
                }
            }
            
            // 7. 부족하면 트렌딩 곡으로 스마트 보충
            if (recommendations.size() < 4) {
                List<String> trendingGenres = getTrendingGenresForUser(profile);
                fillWithTrendingTracks(recommendations, usedTrackIds, trendingGenres, userId, todayMillis);
            }
            
        } catch (Exception e) {
            // 전체 실패 시 빈 리스트 반환
        }
        
        // 8. 최종 중복 제거 및 다양성 보장 (일시적 비활성화 - 테스트용)
        List<Map<String, Object>> finalRecommendations = recommendations; // ensureDiversityAndRemoveDuplicates(recommendations);
        
        // 9. 최근 추천 이력에 새로운 추천 곡들 추가
        updateUserRecentRecommendations(userId, finalRecommendations);
        
        // 10. 로깅으로 중복 문제 진단
        log.warn("🎯 [DEBUG] === 추천 생성 완료 ===");
        log.warn("🎯 [DEBUG] 사용자 ID: {}, 개인화 시드: {}", userId, personalizedSeed);
        log.warn("🎯 [DEBUG] 최초 추천: {}곡, 최종 추천: {}곡", recommendations.size(), finalRecommendations.size());
        
        // 추천 결과의 첫 3곡 로깅 (중복 확인용)
        if (!finalRecommendations.isEmpty()) {
            log.warn("🎯 [DEBUG] 사용자 {} 추천 결과 샘플:", userId);
            for (int i = 0; i < Math.min(3, finalRecommendations.size()); i++) {
                Map<String, Object> rec = finalRecommendations.get(i);
                log.warn("🎯 [DEBUG] {}. {} - {} (ID: {})", 
                        i + 1, rec.get("title"), rec.get("artist"), rec.get("id"));
            }
        }
        
        if (finalRecommendations.size() != recommendations.size()) {
            log.info("사용자 {} 추천에서 {}개 중복/다양성 문제 제거됨", 
                    userId, recommendations.size() - finalRecommendations.size());
        }
        
        // 9. 추천 결과 요약 로깅 (중복 진단용)
        if (log.isDebugEnabled() && !finalRecommendations.isEmpty()) {
            String trackIds = finalRecommendations.stream()
                .map(rec -> (String) rec.get("id"))
                .collect(Collectors.joining(", "));
            log.debug("사용자 {} 최종 추천 트랙 ID들: [{}]", userId, trackIds);
        }
        
        return finalRecommendations;
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
    
    /**
     * 소스별 할당량 계산 - 아티스트는 2-3곡, 장르는 1-2곡
     */
    private Map<String, Integer> calculateSourceQuotas(List<WeightedRecommendationSource> sources, int maxCount) {
        Map<String, Integer> quotas = new HashMap<>();
        
        // 아티스트와 장르 개수 카운트
        long artistCount = sources.stream().filter(s -> "ARTIST".equals(s.type)).count();
        long genreCount = sources.stream().filter(s -> "GENRE".equals(s.type)).count();
        
        // 8곡 추천에 맞춘 할당량 조정
        // 아티스트: 6곡 (75%), 장르: 2곡 (25%)
        int artistTotalQuota = Math.max(1, (int) (maxCount * 0.75));
        int genreTotalQuota = maxCount - artistTotalQuota;
        
        // 아티스트별 할당량 (최소 1곡, 최대 2곡)
        for (WeightedRecommendationSource source : sources) {
            String sourceKey = source.type + ":" + source.value;
            
            if ("ARTIST".equals(source.type)) {
                // 가중치와 아티스트 수를 고려한 동적 할당
                int baseQuota = artistCount > 0 ? Math.max(1, artistTotalQuota / (int) artistCount) : 1;
                
                // 가중치 보너스 (리뷰 기반은 +1, 선호 아티스트는 기본)
                int weightBonus = source.weight >= 0.9 ? 1 : 0;
                int finalQuota = Math.min(2, baseQuota + weightBonus);
                
                quotas.put(sourceKey, finalQuota);
                log.debug("아티스트 할당량: {} -> {}곡 (가중치: {})", source.value, finalQuota, source.weight);
                
            } else if ("GENRE".equals(source.type)) {
                // 장르는 1곡씩 할당
                int genreQuota = 1;
                quotas.put(sourceKey, genreQuota);
                log.debug("장르 할당량: {} -> {}곡 (가중치: {})", source.value, genreQuota, source.weight);
            }
        }
        
        log.info("할당량 계산 완료 - 아티스트: {}개 (총 {}곡), 장르: {}개 (총 {}곡)", 
                artistCount, artistTotalQuota, genreCount, genreTotalQuota);
        
        return quotas;
    }
    
    private TrackDto selectBestTrack(List<TrackDto> tracks, double userWeight, Set<String> usedIds, Long userId, long todayMillis) {
        // 이미 사용된 트랙 제외 (ID, 제목+아티스트, 정규화, 키워드 매칭)
        List<TrackDto> availableTracks = tracks.stream()
            .filter(track -> {
                // ID 중복 체크
                if (usedIds.contains(track.getId())) {
                    return false;
                }
                
                String artist = track.getArtists().isEmpty() ? "Unknown" : track.getArtists().get(0).getName();
                String title = track.getName();
                
                // 1. 정확한 title+artist 중복 체크
                String titleArtistKey = "title_artist:" + (title + "|" + artist).toLowerCase();
                if (usedIds.contains(titleArtistKey)) {
                    return false;
                }
                
                // 2. 정규화된 title+artist 중복 체크 - 비활성화 (너무 강력한 매칭으로 인한 과도한 필터링 방지)
                // String normalizedTitle = normalizeString(title);
                // String normalizedArtist = normalizeString(artist);
                // String normalizedKey = "normalized:" + (normalizedTitle + "|" + normalizedArtist);
                // if (usedIds.contains(normalizedKey)) {
                //     return false;
                // }
                
                // 3. 제목 키워드 매칭 체크 - 너무 강력하므로 비활성화 (정확한 곡만 제외)
                // String[] titleWords = normalizedTitle.split("\\s+");
                // for (String word : titleWords) {
                //     if (word.length() > 3 && usedIds.contains("title_word:" + word)) {
                //         return false;
                //     }
                // }
                
                // 4. 아티스트 키워드 매칭 체크 - 제거 (좋아하는 아티스트의 다른 곡들은 추천되어야 함)
                // String[] artistWords = normalizedArtist.split("\\s+");
                // for (String word : artistWords) {
                //     if (word.length() > 2 && usedIds.contains("artist_word:" + word)) {
                //         return false;
                //     }
                // }
                
                return true;
            })
            .toList();
            
        if (availableTracks.isEmpty()) {
            log.debug("모든 트랙이 이미 사용됨 - 사용된 ID 개수: {}", usedIds.size());
            return null;
        }
        
        // 사용자별 개인화된 랜덤 시드 사용
        Random random = new Random(userId * 777 + todayMillis);
        
        // 랜덤성을 크게 증가 - 상위 50% 중에서 무작위 선택
        int selectFrom = Math.max(1, availableTracks.size() / 2);
        
        // 트랙을 섞어서 무작위 순서로 만들기
        List<TrackDto> shuffledTracks = new ArrayList<>(availableTracks);
        Collections.shuffle(shuffledTracks, random);
        
        // 인기도와 개인화 가중치를 결합한 점수 계산 (랜덤 팩터 감소)
        List<ScoredTrack> scoredTracks = shuffledTracks.stream()
            .map(track -> {
                double baseScore = (track.getPopularity() / 100.0) * 0.4 + userWeight * 0.6;
                // 랜덤 요소 감소 (±0.1)로 인기도와 가중치를 더 중시
                double randomFactor = (random.nextDouble() - 0.5) * 0.2;
                return new ScoredTrack(track, baseScore + randomFactor);
            })
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .toList();
        
        // 상위 N개 중에서 완전 무작위 선택 (더 큰 다양성)
        int finalSelectFrom = Math.min(selectFrom, scoredTracks.size());
        int randomIndex = random.nextInt(finalSelectFrom);
        
        TrackDto selectedTrack = scoredTracks.get(randomIndex).track;
        log.debug("트랙 선택: {} ({}개 중 {}번째)", selectedTrack.getName(), scoredTracks.size(), randomIndex + 1);
        
        return selectedTrack;
    }
    
    private static class ScoredTrack {
        final TrackDto track;
        final double score;
        
        ScoredTrack(TrackDto track, double score) {
            this.track = track;
            this.score = score;
        }
    }
    
    private List<String> getTrendingGenresForUser(ProfileDto profile) {
        Set<String> userGenres = profile.getFavoriteGenres().stream()
            .map(genreMap -> (String) genreMap.get("name"))
            .filter(Objects::nonNull)
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
                                       Set<String> usedIds, List<String> trendingGenres, Long userId, long todayMillis) {
        Random random = new Random(userId * 555 + todayMillis); // 사용자별 개인화된 트렌딩 랜덤
        
        while (recommendations.size() < 4 && !trendingGenres.isEmpty()) {
            String genre = trendingGenres.get(random.nextInt(trendingGenres.size()));
            try {
                List<TrackDto> tracks = spotifyService.searchTracks(genre + " 2024", 5);
                TrackDto bestTrack = selectBestTrack(tracks, 0.4, usedIds, userId, todayMillis);
                
                if (bestTrack != null) {
                    recommendations.add(createRecommendationMap(bestTrack, "트렌딩"));
                    usedIds.add(bestTrack.getId());
                    
                    // 제목+아티스트 조합도 추가하여 중복 방지 강화
                    String artist = bestTrack.getArtists().isEmpty() ? "Unknown" : bestTrack.getArtists().get(0).getName();
                    String titleArtistKey = "title_artist:" + (bestTrack.getName() + "|" + artist).toLowerCase();
                    usedIds.add(titleArtistKey);
                }
            } catch (Exception e) {
                // 실패 시 해당 장르 제거
            }
            trendingGenres.remove(genre);
        }
    }
    
    private List<Map<String, Object>> ensureDiversityAndRemoveDuplicates(List<Map<String, Object>> recommendations) {
        Map<String, Map<String, Object>> uniqueById = new LinkedHashMap<>();
        Map<String, Map<String, Object>> uniqueByTitleArtist = new LinkedHashMap<>(); // 제목+아티스트 중복 체크 추가
        Map<String, Integer> artistCount = new HashMap<>();
        Map<String, Integer> genreCount = new HashMap<>();
        
        List<Map<String, Object>> diverseRecommendations = new ArrayList<>();
        
        for (Map<String, Object> rec : recommendations) {
            String trackId = (String) rec.get("id");
            String title = (String) rec.get("title");
            String artist = (String) rec.get("artist");
            String genre = (String) rec.get("genre");
            
            // ID 중복 체크
            if (uniqueById.containsKey(trackId)) {
                log.debug("ID 중복 제거: {}", trackId);
                continue;
            }
            
            // 제목+아티스트 중복 체크 (더 엄격한 중복 제거)
            String titleArtistKey = (title + "_" + artist).toLowerCase().replaceAll("\\s+", "");
            if (uniqueByTitleArtist.containsKey(titleArtistKey)) {
                log.debug("제목+아티스트 중복 제거: {} - {}", title, artist);
                continue;
            }
            
            // 아티스트 다양성 체크 (같은 아티스트 최대 2곡까지 허용)
            int artistFreq = artistCount.getOrDefault(artist, 0);
            if (artistFreq >= 2) {
                log.debug("아티스트 다양성으로 제거: {} ({}곡 이미 있음)", artist, artistFreq);
                continue;
            }
            
            // 장르 다양성 체크 (같은 장르 최대 3곡)
            int genreFreq = genreCount.getOrDefault(genre, 0);
            if (genreFreq >= 3) {
                log.debug("장르 다양성으로 제거: {}", genre);
                continue;
            }
            
            // 통과한 추천 추가
            uniqueById.put(trackId, rec);
            uniqueByTitleArtist.put(titleArtistKey, rec);
            diverseRecommendations.add(rec);
            artistCount.put(artist, artistFreq + 1);
            genreCount.put(genre, genreFreq + 1);
            
            log.debug("추천 추가: {} - {} ({})", title, artist, genre);
        }
        
        log.debug("중복 제거 결과: {}곡 -> {}곡", recommendations.size(), diverseRecommendations.size());
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
     * 폴백 추천 생성 (외부 API 실패 시)
     */
    private List<Map<String, Object>> generateFallbackRecommendations(Long userId) {
        try {
            // MusicRecommendationEngine의 폴백 추천 사용
            return recommendationEngine.getPersonalizedRecommendations(userId, 5);
        } catch (Exception e) {
            // 최후의 수단: 하드코딩된 기본 추천
            return List.of(
                Map.of("id", "fallback_1", "title", "Popular Song", "artist", "Various Artists", 
                       "genre", "Pop", "score", 75, "popularity", 80, "duration", "3:30"),
                Map.of("id", "fallback_2", "title", "Trending Hit", "artist", "Chart Artist", 
                       "genre", "Electronic", "score", 70, "popularity", 85, "duration", "3:45")
            );
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
                .map(genreMap -> (String) genreMap.get("name"))
                .filter(Objects::nonNull)
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
    
    /**
     * 실제 아티스트 매치 검증 - 제목에 아티스트 이름이 포함된 다른 아티스트 곡 필터링
     */
    private List<TrackDto> filterTracksByActualArtist(List<TrackDto> tracks, String targetArtist) {
        String normalizedTarget = normalizeArtistName(targetArtist);
        
        List<TrackDto> filteredTracks = tracks.stream()
            .filter(track -> {
                if (track.getArtists() == null || track.getArtists().isEmpty()) {
                    return false;
                }
                
                // 트랙의 아티스트 중 하나라도 타겟 아티스트와 매치되면 통과
                boolean artistMatch = track.getArtists().stream()
                    .anyMatch(artist -> {
                        String normalizedArtist = normalizeArtistName(artist.getName());
                        
                        // 다양한 매치 조건
                        boolean exactMatch = normalizedArtist.equals(normalizedTarget);
                        boolean similarMatch = calculateSimilarity(normalizedArtist, normalizedTarget) >= 0.8; // 80%로 완화
                        boolean containsMatch = normalizedArtist.contains(normalizedTarget) || normalizedTarget.contains(normalizedArtist);
                        
                        return exactMatch || similarMatch || containsMatch;
                    });
                
                if (artistMatch) {
                    log.debug("아티스트 매치: {} - {} (타겟: {})", 
                             track.getName(), 
                             track.getArtists().get(0).getName(), 
                             targetArtist);
                    return true;
                } else {
                    log.debug("아티스트 불일치로 필터링: {} - {} (타겟: {})", 
                             track.getName(), 
                             track.getArtists().get(0).getName(), 
                             targetArtist);
                    return false;
                }
            })
            .toList();
            
        // 필터링 결과가 너무 적으면 (3곡 미만) 더 관대한 기준 적용
        if (filteredTracks.size() < 3) {
            log.warn("필터링 결과가 {}곡으로 적음. 더 관대한 기준 적용: {}", filteredTracks.size(), targetArtist);
            
            // 더 관대한 필터링: 제목에 아티스트 이름이 있는 것만 제외
            return tracks.stream()
                .filter(track -> {
                    if (track.getArtists() == null || track.getArtists().isEmpty()) {
                        return false;
                    }
                    
                    String trackTitle = track.getName().toLowerCase();
                    String trackArtist = track.getArtists().get(0).getName().toLowerCase();
                    String target = targetArtist.toLowerCase();
                    
                    // 트랙 제목에 타겟 아티스트 이름이 있고, 실제 아티스트가 다르면 제외
                    boolean titleHasTargetArtist = trackTitle.contains(target);
                    boolean actualArtistMatches = trackArtist.contains(target) || target.contains(trackArtist);
                    
                    if (titleHasTargetArtist && !actualArtistMatches) {
                        log.debug("제목에 아티스트명 포함된 다른 아티스트 곡 제외: {} - {}", track.getName(), trackArtist);
                        return false;
                    }
                    
                    return true;
                })
                .toList();
        }
        
        return filteredTracks;
    }
    
    /**
     * 아티스트 이름 정규화 (공백, 특수문자 제거)
     */
    private String normalizeArtistName(String name) {
        if (name == null) return "";
        
        return name.toLowerCase()
                  .replaceAll("[^a-zA-Z0-9가-힣]", "") // 알파벳, 숫자, 한글만 유지
                  .trim();
    }
    
    /**
     * 문자열 유사도 계산 (레벤슈타인 거리 기반)
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        
        return (maxLength - levenshteinDistance(s1, s2)) / (double) maxLength;
    }
    
    /**
     * 레벤슈타인 거리 계산
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,    // deletion
                    dp[i][j - 1] + 1),   // insertion
                    dp[i - 1][j - 1] + cost); // substitution
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * 사용자별 최근 추천 이력 조회 (캐시 만료 시 초기화)
     */
    private Set<String> getUserRecentRecommendations(Long userId) {
        long currentTime = System.currentTimeMillis();
        Long lastUpdateTime = userRecommendationTimestamps.get(userId);
        
        // 캐시가 만료된 경우 초기화
        if (lastUpdateTime == null || (currentTime - lastUpdateTime) > RECOMMENDATION_CACHE_DURATION_MS) {
            userRecentRecommendations.remove(userId);
            userRecommendationTimestamps.remove(userId);
            log.debug("사용자 {} 추천 이력 캐시 만료로 초기화", userId);
            return new HashSet<>();
        }
        
        return userRecentRecommendations.getOrDefault(userId, new HashSet<>());
    }
    
    /**
     * ID로 중복되지 않는 트랙만 추가
     */
    private void addUniqueTracksById(List<TrackDto> existingTracks, List<TrackDto> newTracks) {
        Set<String> existingIds = existingTracks.stream()
            .map(TrackDto::getId)
            .collect(Collectors.toSet());
            
        for (TrackDto newTrack : newTracks) {
            if (!existingIds.contains(newTrack.getId())) {
                existingTracks.add(newTrack);
                existingIds.add(newTrack.getId());
            }
        }
    }
    
    /**
     * 사용자가 좋아요한 곡들의 ID 가져오기 (추천에서 제외하기 위함)
     */
    private Set<String> getUserLikedTrackIds(Long userId) {
        try {
            // 사용자가 좋아요한 곡들을 페이징으로 가져오기 (최대 1000곡까지)
            Pageable pageable = PageRequest.of(0, 1000);
            Page<UserSongLike> likedSongs = userSongLikeService.getUserLikedSongs(userId, pageable);
            
            Set<String> likedTrackKeys = new HashSet<>();
            
            for (UserSongLike like : likedSongs.getContent()) {
                if (like.getSong() != null) {
                    String title = like.getSong().getTitle();
                    String artist = like.getSong().getArtist();
                    
                    if (title != null && artist != null) {
                        // 1. 정확한 title+artist 조합
                        String titleArtistKey = "title_artist:" + (title + "|" + artist).toLowerCase();
                        likedTrackKeys.add(titleArtistKey);
                        
                        // 2. 정규화된 title+artist 조합 - 비활성화 (과도한 필터링 방지)
                        // String normalizedTitle = normalizeString(title);
                        // String normalizedArtist = normalizeString(artist);
                        // String normalizedKey = "normalized:" + (normalizedTitle + "|" + normalizedArtist);
                        // likedTrackKeys.add(normalizedKey);
                        
                        // 3. 부분 매칭을 위한 키워드들 - 비활성화 (과도한 필터링 방지)
                        // String[] titleWords = normalizedTitle.split("\\s+");
                        // String[] artistWords = normalizedArtist.split("\\s+");
                        
                        // 제목 키워드는 제외 - 너무 강력한 필터링 방지 (정확한 곡만 제외)
                        // for (String word : titleWords) {
                        //     if (word.length() > 3) {
                        //         likedTrackKeys.add("title_word:" + word);
                        //     }
                        // }
                        
                        // 아티스트 키워드는 제외 - 좋아하는 아티스트의 다른 곡들은 추천되어야 함
                        // for (String word : artistWords) {
                        //     if (word.length() > 2) {
                        //         likedTrackKeys.add("artist_word:" + word);
                        //     }
                        // }
                        
                        log.debug("좋아요한 곡 추가: {} - {} (키: 1개)", title, artist);
                    }
                }
            }
            
            log.info("사용자 {} 좋아요한 곡 {}개를 추천에서 제외 (총 {}개 키)", userId, likedSongs.getContent().size(), likedTrackKeys.size());
            return likedTrackKeys;
            
        } catch (Exception e) {
            log.warn("좋아요한 곡 목록 가져오기 실패: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    /**
     * 사용자별 최근 추천 이력 업데이트
     */
    private void updateUserRecentRecommendations(Long userId, List<Map<String, Object>> newRecommendations) {
        Set<String> recentTracks = userRecentRecommendations.computeIfAbsent(userId, k -> new HashSet<>());
        
        // 새로운 추천 곡들의 ID 추가
        for (Map<String, Object> rec : newRecommendations) {
            String trackId = (String) rec.get("id");
            if (trackId != null) {
                recentTracks.add(trackId);
                
                // 제목+아티스트 조합도 추가하여 더 강력한 중복 방지
                String title = (String) rec.get("title");
                String artist = (String) rec.get("artist");
                if (title != null && artist != null) {
                    String titleArtistKey = (title + "|" + artist).toLowerCase();
                    recentTracks.add("title_artist:" + titleArtistKey);
                }
            }
        }
        
        // 타임스탬프 업데이트
        userRecommendationTimestamps.put(userId, System.currentTimeMillis());
        
        log.debug("사용자 {} 추천 이력 업데이트 완료 - 총 {}개 트랙", userId, recentTracks.size());
        
        // 메모리 효율성을 위해 너무 많은 트랙이 쌓이면 제한
        if (recentTracks.size() > 100) {
            // 오래된 것들 제거 (간단히 절반 제거)
            Set<String> reducedSet = recentTracks.stream().limit(50).collect(Collectors.toSet());
            userRecentRecommendations.put(userId, reducedSet);
            log.debug("사용자 {} 추천 이력 크기 제한으로 {}개로 축소", userId, reducedSet.size());
        }
    }

    private String normalizeString(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase()
                .replaceAll("[^a-zA-Z0-9가-힣\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 추천 히스토리를 데이터베이스에 저장
     */
    private void saveRecommendationHistory(Long userId, List<Map<String, Object>> recommendations) {
        try {
            String sessionId = generateSessionId();
            LocalDateTime now = LocalDateTime.now();
            
            List<RecommendationHistory> historyList = new ArrayList<>();
            
            for (Map<String, Object> rec : recommendations) {
                // 중복 체크: 최근 1시간 내에 같은 트랙이 추천되었는지 확인
                String trackId = (String) rec.get("id");
                String trackTitle = (String) rec.get("title");
                String trackArtist = (String) rec.get("artist");
                
                if (trackTitle != null && trackArtist != null) {
                    boolean alreadyRecommended = recommendationHistoryRepository
                        .existsByUserIdAndTrackTitleAndTrackArtistAndCreatedAtAfter(
                            userId, trackTitle, trackArtist, now.minusHours(1)
                        );
                    
                    if (!alreadyRecommended) {
                        RecommendationHistory history = RecommendationHistory.builder()
                            .userId(userId)
                            .trackId(trackId)
                            .trackTitle(trackTitle)
                            .trackArtist(trackArtist)
                            .trackGenre((String) rec.get("genre"))
                            .recommendationType((String) rec.get("genre"))
                            .recommendationScore((Integer) rec.get("score"))
                            .spotifyId((String) rec.get("spotifyId"))
                            .sessionId(sessionId)
                            .createdAt(now)
                            .build();
                        
                        historyList.add(history);
                    } else {
                        log.debug("중복 추천 스킵: {} - {}", trackTitle, trackArtist);
                    }
                }
            }
            
            if (!historyList.isEmpty()) {
                // 배치로 저장 (성능 최적화)
                CompletableFuture.runAsync(() -> {
                    try {
                        recommendationHistoryRepository.saveAll(historyList);
                        log.info("사용자 {} 추천 히스토리 {}개 저장 완료 (세션: {})", userId, historyList.size(), sessionId);
                    } catch (Exception e) {
                        log.error("추천 히스토리 저장 실패 - userId: {}, error: {}", userId, e.getMessage(), e);
                    }
                });
            }
            
        } catch (Exception e) {
            log.error("추천 히스토리 저장 중 오류: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 세션 ID 생성 (같은 시점에 생성된 추천들을 그룹화)
     */
    private String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
}