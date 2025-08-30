package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.*;
import com.example.musicrecommendation.repository.ReviewHelpfulRepository;
import com.example.musicrecommendation.repository.UserProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 향상된 음악 기반 사용자 매칭 서비스
 * 
 * 매칭 가중치:
 * - 도움이 됨 매칭: 25%
 * - 좋아요 음악 매칭: 20%
 * - 참여 페스티벌: 15%
 * - 선호 아티스트: 15% 
 * - 선호 장르: 15%
 * - 음악 무드: 5%
 * - 기타 음악 특성: 5%
 */
@Slf4j
@Service
@Transactional
public class MusicMatchingService {

    private final UserRepository userRepository;
    private final UserMatchRepository userMatchRepository;
    private final UserSongLikeRepository userSongLikeRepository;
    private final UserPreferenceService userPreferenceService;
    private final MusicSimilarityService musicSimilarityService;
    private final NotificationService notificationService;
    private final ReviewHelpfulRepository reviewHelpfulRepository;
    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper;

    // 매칭 가중치 상수 (총합 100%)
    private static final double HELPFUL_WEIGHT = 0.25;    // 도움이 됨 매칭
    private static final double LIKED_SONGS_WEIGHT = 0.20; // 좋아요 음악 매칭
    private static final double FESTIVAL_WEIGHT = 0.15;   // 참여 페스티벌
    private static final double ARTIST_WEIGHT = 0.15;     // 선호 아티스트
    private static final double GENRE_WEIGHT = 0.15;      // 선호 장르
    private static final double MOOD_WEIGHT = 0.05;       // 음악 무드
    private static final double PREFERENCE_WEIGHT = 0.05;  // 기타 음악 특성

    public MusicMatchingService(UserRepository userRepository,
                                UserMatchRepository userMatchRepository,
                                UserSongLikeRepository userSongLikeRepository,
                                UserPreferenceService userPreferenceService,
                                MusicSimilarityService musicSimilarityService,
                                NotificationService notificationService,
                                ReviewHelpfulRepository reviewHelpfulRepository,
                                UserProfileRepository userProfileRepository,
                                ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.userMatchRepository = userMatchRepository;
        this.userSongLikeRepository = userSongLikeRepository;
        this.userPreferenceService = userPreferenceService;
        this.musicSimilarityService = musicSimilarityService;
        this.notificationService = notificationService;
        this.reviewHelpfulRepository = reviewHelpfulRepository;
        this.userProfileRepository = userProfileRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 향상된 매칭 후보 찾기 - 다중 요소 기반
     */
    public List<UserMatch> findMatchCandidates(Long userId, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        log.info("=== 매칭 후보 찾기 시작: 사용자 ID {} ===", userId);

        // 모든 다른 사용자들과 다중 요소 유사도 계산
        List<User> allUsers = userRepository.findAll();
        List<UserMatch> candidates = new ArrayList<>();

        for (User otherUser : allUsers) {
            if (otherUser.getId().equals(userId)) continue;

            // 이미 매칭된 사용자는 제외
            Optional<UserMatch> existingMatch = userMatchRepository.findMatchBetweenUsers(userId, otherUser.getId());
            if (existingMatch.isPresent()) continue;

            try {
                // 향상된 유사도 계산
                double totalSimilarity = calculateEnhancedSimilarity(userId, otherUser.getId());
                
                if (totalSimilarity >= 0.4) { // 임계값 0.4 이상만
                    String reason = generateEnhancedMatchReason(userId, otherUser.getId(), totalSimilarity);
                    UserMatch match = new UserMatch(userId, otherUser.getId(), totalSimilarity, reason);
                    candidates.add(match);
                    
                    log.debug("매칭 후보 추가: {} <-> {}, 유사도: {:.3f}", 
                            userId, otherUser.getId(), totalSimilarity);
                }
            } catch (Exception e) {
                log.warn("매칭 계산 실패: {} <-> {}, 오류: {}", 
                        userId, otherUser.getId(), e.getMessage());
            }
        }

        // 유사도 순으로 정렬하고 제한
        List<UserMatch> result = candidates.stream()
                .sorted((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()))
                .limit(limit)
                .collect(Collectors.toList());

        log.info("매칭 후보 {}개 발견 (상위 {}개 반환)", candidates.size(), result.size());
        return result;
    }

    /**
     * 향상된 매칭 생성 및 저장
     */
    public UserMatch createMatch(Long user1Id, Long user2Id) {
        log.info("매칭 생성 요청: {} <-> {}", user1Id, user2Id);
        
        // 이미 존재하는 매칭인지 확인
        Optional<UserMatch> existingMatch = userMatchRepository.findMatchBetweenUsers(user1Id, user2Id);
        if (existingMatch.isPresent()) {
            log.debug("기존 매칭 반환: ID {}", existingMatch.get().getId());
            return existingMatch.get();
        }

        try {
            // 향상된 유사도 계산
            double similarity = calculateEnhancedSimilarity(user1Id, user2Id);
            String reason = generateEnhancedMatchReason(user1Id, user2Id, similarity);
            
            // 매칭 생성
            UserMatch match = new UserMatch(user1Id, user2Id, similarity, reason);

            // 공통 좋아요 곡 수 계산 (기존 로직 유지)
            int commonLikedSongs = calculateCommonLikedSongs(user1Id, user2Id);
            match.setCommonLikedSongs(commonLikedSongs);

            var savedMatch = userMatchRepository.save(match);
            log.info("새 매칭 생성 완료: ID {}, 유사도 {:.3f}", savedMatch.getId(), similarity);
            
            // 매칭 알림 전송 (비동기)
            try {
                notificationService.sendMatchNotification(user1Id, user2Id, similarity, reason);
                notificationService.sendMatchNotification(user2Id, user1Id, similarity, reason);
            } catch (Exception e) {
                log.warn("매칭 알림 전송 실패: {}", e.getMessage());
                // 알림 전송 실패해도 매칭 생성은 성공으로 처리
            }
            
            return savedMatch;
        } catch (Exception e) {
            log.error("매칭 생성 실패: {} <-> {}, 오류: {}", user1Id, user2Id, e.getMessage());
            throw new RuntimeException("매칭 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 향상된 다중 요소 유사도 계산 (좋아요 음악, 페스티벌 & 무드 포함)
     */
    private double calculateEnhancedSimilarity(Long user1Id, Long user2Id) {
        double helpfulSimilarity = calculateHelpfulSimilarity(user1Id, user2Id);
        double likedSongsSimilarity = calculateLikedSongsSimilarity(user1Id, user2Id);
        double festivalSimilarity = calculateFestivalSimilarity(user1Id, user2Id);
        double artistSimilarity = calculateArtistSimilarity(user1Id, user2Id);
        double genreSimilarity = calculateGenreSimilarity(user1Id, user2Id);
        double moodSimilarity = calculateMoodSimilarity(user1Id, user2Id);
        double preferenceSimilarity = calculatePreferenceSimilarity(user1Id, user2Id);

        double totalSimilarity = 
            (helpfulSimilarity * HELPFUL_WEIGHT) +
            (likedSongsSimilarity * LIKED_SONGS_WEIGHT) +
            (festivalSimilarity * FESTIVAL_WEIGHT) +
            (artistSimilarity * ARTIST_WEIGHT) +
            (genreSimilarity * GENRE_WEIGHT) + 
            (moodSimilarity * MOOD_WEIGHT) +
            (preferenceSimilarity * PREFERENCE_WEIGHT);

        log.debug("유사도 상세 - 도움됨:{:.3f}, 좋아요곡:{:.3f}, 페스티벌:{:.3f}, 아티스트:{:.3f}, 장르:{:.3f}, 무드:{:.3f}, 기타:{:.3f} = 총합:{:.3f}",
                helpfulSimilarity, likedSongsSimilarity, festivalSimilarity, artistSimilarity, genreSimilarity, moodSimilarity, preferenceSimilarity, totalSimilarity);

        return Math.min(0.95, totalSimilarity); // 최대 0.95로 제한
    }

    /**
     * 도움이 됨 기반 유사도 (가중치 40%)
     */
    private double calculateHelpfulSimilarity(Long user1Id, Long user2Id) {
        try {
            // 사용자1이 도움이 됨을 누른 리뷰들
            List<ReviewHelpful> user1Helpfuls = reviewHelpfulRepository.findByUserId(user1Id);
            List<ReviewHelpful> user2Helpfuls = reviewHelpfulRepository.findByUserId(user2Id);

            if (user1Helpfuls.isEmpty() || user2Helpfuls.isEmpty()) {
                return 0.1; // 기본값
            }

            // 공통으로 도움이 됨을 누른 리뷰 수
            Set<Long> user1ReviewIds = user1Helpfuls.stream()
                    .map(ReviewHelpful::getReviewId)
                    .collect(Collectors.toSet());
            Set<Long> user2ReviewIds = user2Helpfuls.stream()
                    .map(ReviewHelpful::getReviewId)
                    .collect(Collectors.toSet());

            Set<Long> commonReviews = new HashSet<>(user1ReviewIds);
            commonReviews.retainAll(user2ReviewIds);

            // Jaccard 유사도 계산
            Set<Long> unionReviews = new HashSet<>(user1ReviewIds);
            unionReviews.addAll(user2ReviewIds);

            double similarity = unionReviews.isEmpty() ? 0.0 : 
                    (double) commonReviews.size() / unionReviews.size();

            // 보너스: 공통 리뷰가 많을수록 추가 점수
            double bonus = Math.min(0.3, commonReviews.size() * 0.1);
            
            return Math.min(1.0, similarity + bonus);
        } catch (Exception e) {
            log.warn("도움이 됨 유사도 계산 실패: {}", e.getMessage());
            return 0.1;
        }
    }

    /**
     * 좋아요 음악 기반 유사도 (가중치 20%)
     */
    private double calculateLikedSongsSimilarity(Long user1Id, Long user2Id) {
        try {
            // 사용자들이 좋아요한 곡 ID 목록 조회
            List<Long> user1SongIds = userSongLikeRepository.findLikedSongIdsByUserId(user1Id);
            List<Long> user2SongIds = userSongLikeRepository.findLikedSongIdsByUserId(user2Id);

            if (user1SongIds.isEmpty() || user2SongIds.isEmpty()) {
                return 0.1; // 기본값 - 좋아요 데이터가 없으면 낮은 점수
            }

            // 공통 좋아요 음악 계산
            Set<Long> user1SongSet = new HashSet<>(user1SongIds);
            Set<Long> user2SongSet = new HashSet<>(user2SongIds);
            Set<Long> commonSongs = new HashSet<>(user1SongSet);
            commonSongs.retainAll(user2SongSet);

            // 전체 좋아요 음악 (합집합)
            Set<Long> allSongs = new HashSet<>(user1SongSet);
            allSongs.addAll(user2SongSet);

            if (allSongs.isEmpty()) {
                return 0.1;
            }

            // Jaccard 유사도 계산
            double jaccardSimilarity = (double) commonSongs.size() / allSongs.size();

            // 보너스: 공통 좋아요 음악이 많을수록 추가 점수
            double bonus = Math.min(0.3, commonSongs.size() * 0.05);
            
            // 아티스트별 유사도도 고려 (같은 아티스트의 다른 곡들)
            double artistBonus = calculateArtistBasedLikesSimilarity(user1Id, user2Id);
            
            double finalSimilarity = Math.min(1.0, jaccardSimilarity + bonus + artistBonus);
            
            log.debug("좋아요 음악 유사도 - 공통곡:{}, 전체곡:{}, Jaccard:{:.3f}, 보너스:{:.3f}, 아티스트보너스:{:.3f} = {:.3f}", 
                    commonSongs.size(), allSongs.size(), jaccardSimilarity, bonus, artistBonus, finalSimilarity);
            
            return finalSimilarity;
        } catch (Exception e) {
            log.warn("좋아요 음악 유사도 계산 실패: {}", e.getMessage());
            return 0.1;
        }
    }

    /**
     * 아티스트 기반 좋아요 음악 유사도 보너스 계산
     */
    private double calculateArtistBasedLikesSimilarity(Long user1Id, Long user2Id) {
        try {
            // 각 사용자가 좋아한 곡들의 아티스트 정보를 조회하기 위해 전체 UserSongLike 조회
            List<UserSongLike> user1Likes = userSongLikeRepository.findByUserIdOrderByLikedAtDesc(user1Id, org.springframework.data.domain.Pageable.unpaged()).getContent();
            List<UserSongLike> user2Likes = userSongLikeRepository.findByUserIdOrderByLikedAtDesc(user2Id, org.springframework.data.domain.Pageable.unpaged()).getContent();
            
            if (user1Likes.isEmpty() || user2Likes.isEmpty()) {
                return 0.0;
            }

            // 각 사용자가 좋아한 아티스트들 추출
            Set<String> user1Artists = user1Likes.stream()
                    .map(like -> like.getSong().getArtist())
                    .filter(Objects::nonNull)
                    .map(artist -> artist.toLowerCase().trim())
                    .collect(Collectors.toSet());
            
            Set<String> user2Artists = user2Likes.stream()
                    .map(like -> like.getSong().getArtist())
                    .filter(Objects::nonNull)
                    .map(artist -> artist.toLowerCase().trim())
                    .collect(Collectors.toSet());

            if (user1Artists.isEmpty() || user2Artists.isEmpty()) {
                return 0.0;
            }

            // 공통 아티스트 계산
            Set<String> commonArtists = new HashSet<>(user1Artists);
            commonArtists.retainAll(user2Artists);

            // 공통 아티스트 수에 따른 보너스 (최대 0.2)
            return Math.min(0.2, commonArtists.size() * 0.03);
        } catch (Exception e) {
            log.debug("아티스트 기반 좋아요 유사도 보너스 계산 실패: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * 선호 아티스트 기반 유사도 (가중치 15%)
     */
    private double calculateArtistSimilarity(Long user1Id, Long user2Id) {
        try {
            Optional<UserProfile> profile1 = userProfileRepository.findByUserId(user1Id);
            Optional<UserProfile> profile2 = userProfileRepository.findByUserId(user2Id);

            if (profile1.isEmpty() || profile2.isEmpty()) {
                return 0.2; // 기본값
            }

            List<Map<String, Object>> artists1 = profile1.get().getFavoriteArtists();
            List<Map<String, Object>> artists2 = profile2.get().getFavoriteArtists();

            if (artists1.isEmpty() || artists2.isEmpty()) {
                return 0.2;
            }

            // 아티스트 이름 추출
            Set<String> artistNames1 = artists1.stream()
                    .map(artist -> (String) artist.get("name"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Set<String> artistNames2 = artists2.stream()
                    .map(artist -> (String) artist.get("name"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // Jaccard 유사도
            Set<String> common = new HashSet<>(artistNames1);
            common.retainAll(artistNames2);
            
            Set<String> union = new HashSet<>(artistNames1);
            union.addAll(artistNames2);

            return union.isEmpty() ? 0.2 : (double) common.size() / union.size();
        } catch (Exception e) {
            log.warn("아티스트 유사도 계산 실패: {}", e.getMessage());
            return 0.2;
        }
    }

    /**
     * 선호 장르 기반 유사도 (가중치 20%)
     */
    private double calculateGenreSimilarity(Long user1Id, Long user2Id) {
        try {
            Optional<UserProfile> profile1 = userProfileRepository.findByUserId(user1Id);
            Optional<UserProfile> profile2 = userProfileRepository.findByUserId(user2Id);

            if (profile1.isEmpty() || profile2.isEmpty()) {
                return 0.3; // 기본값
            }

            List<Map<String, Object>> genres1 = profile1.get().getFavoriteGenres();
            List<Map<String, Object>> genres2 = profile2.get().getFavoriteGenres();

            if (genres1.isEmpty() || genres2.isEmpty()) {
                return 0.3;
            }

            // 장르 이름 추출
            Set<String> genreNames1 = genres1.stream()
                    .map(genre -> (String) genre.get("name"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Set<String> genreNames2 = genres2.stream()
                    .map(genre -> (String) genre.get("name"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // Jaccard 유사도
            Set<String> common = new HashSet<>(genreNames1);
            common.retainAll(genreNames2);
            
            Set<String> union = new HashSet<>(genreNames1);
            union.addAll(genreNames2);

            return union.isEmpty() ? 0.3 : (double) common.size() / union.size();
        } catch (Exception e) {
            log.warn("장르 유사도 계산 실패: {}", e.getMessage());
            return 0.3;
        }
    }

    /**
     * 참여 페스티벌 기반 유사도 (가중치 20%)
     */
    private double calculateFestivalSimilarity(Long user1Id, Long user2Id) {
        try {
            Optional<UserProfile> profile1 = userProfileRepository.findByUserId(user1Id);
            Optional<UserProfile> profile2 = userProfileRepository.findByUserId(user2Id);

            if (profile1.isEmpty() || profile2.isEmpty()) {
                return 0.2; // 기본값
            }

            List<String> festivals1 = profile1.get().getAttendedFestivals();
            List<String> festivals2 = profile2.get().getAttendedFestivals();

            if (festivals1 == null || festivals2 == null || festivals1.isEmpty() || festivals2.isEmpty()) {
                return 0.2;
            }

            // 페스티벌 이름 정규화 (대소문자 무시, 공백 제거)
            Set<String> normalizedFestivals1 = festivals1.stream()
                    .filter(Objects::nonNull)
                    .map(festival -> festival.toLowerCase().trim())
                    .collect(Collectors.toSet());
            Set<String> normalizedFestivals2 = festivals2.stream()
                    .filter(Objects::nonNull)
                    .map(festival -> festival.toLowerCase().trim())
                    .collect(Collectors.toSet());

            // Jaccard 유사도 계산
            Set<String> common = new HashSet<>(normalizedFestivals1);
            common.retainAll(normalizedFestivals2);
            
            Set<String> union = new HashSet<>(normalizedFestivals1);
            union.addAll(normalizedFestivals2);

            double similarity = union.isEmpty() ? 0.2 : (double) common.size() / union.size();
            
            // 보너스: 공통 페스티벌이 많을수록 추가 점수 (페스티벌은 특별한 경험)
            double bonus = Math.min(0.4, common.size() * 0.15);
            
            return Math.min(1.0, similarity + bonus);
        } catch (Exception e) {
            log.warn("페스티벌 유사도 계산 실패: {}", e.getMessage());
            return 0.2;
        }
    }

    /**
     * 음악 무드 기반 유사도 (가중치 10%)
     */
    private double calculateMoodSimilarity(Long user1Id, Long user2Id) {
        try {
            Optional<UserProfile> profile1 = userProfileRepository.findByUserId(user1Id);
            Optional<UserProfile> profile2 = userProfileRepository.findByUserId(user2Id);

            if (profile1.isEmpty() || profile2.isEmpty()) {
                return 0.3; // 기본값
            }

            Map<String, Object> preferences1 = profile1.get().getMusicPreferences();
            Map<String, Object> preferences2 = profile2.get().getMusicPreferences();

            if (preferences1 == null || preferences2 == null) {
                return 0.3;
            }

            // moodPreferences 추출
            Object moodPref1 = preferences1.get("moodPreferences");
            Object moodPref2 = preferences2.get("moodPreferences");

            if (moodPref1 == null || moodPref2 == null) {
                return 0.3;
            }

            Set<String> moods1 = extractMoods(moodPref1);
            Set<String> moods2 = extractMoods(moodPref2);

            if (moods1.isEmpty() || moods2.isEmpty()) {
                return 0.3;
            }

            // Jaccard 유사도 계산
            Set<String> common = new HashSet<>(moods1);
            common.retainAll(moods2);
            
            Set<String> union = new HashSet<>(moods1);
            union.addAll(moods2);

            return union.isEmpty() ? 0.3 : (double) common.size() / union.size();
        } catch (Exception e) {
            log.warn("무드 유사도 계산 실패: {}", e.getMessage());
            return 0.3;
        }
    }

    /**
     * 무드 데이터 추출 헬퍼 메서드
     */
    private Set<String> extractMoods(Object moodPref) {
        Set<String> moods = new HashSet<>();
        try {
            if (moodPref instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> moodList = (List<Object>) moodPref;
                for (Object mood : moodList) {
                    if (mood instanceof String) {
                        moods.add(((String) mood).toLowerCase().trim());
                    } else if (mood instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> moodMap = (Map<String, Object>) mood;
                        if (moodMap.containsKey("name")) {
                            moods.add(String.valueOf(moodMap.get("name")).toLowerCase().trim());
                        }
                    }
                }
            } else if (moodPref instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> moodMap = (Map<String, Object>) moodPref;
                moodMap.values().forEach(value -> {
                    if (value instanceof String) {
                        moods.add(((String) value).toLowerCase().trim());
                    }
                });
            }
        } catch (Exception e) {
            log.debug("무드 데이터 추출 실패: {}", e.getMessage());
        }
        return moods;
    }

    /**
     * 기타 음악 선호도 특성 유사도 (가중치 5%)
     */
    private double calculatePreferenceSimilarity(Long user1Id, Long user2Id) {
        try {
            // UserPreferenceProfile을 통한 벡터 유사도 계산
            // 여기서는 간단한 구현만 제공
            return 0.5 + (Math.random() * 0.3 - 0.15); // 임시 구현
        } catch (Exception e) {
            log.warn("선호도 유사도 계산 실패: {}", e.getMessage());
            return 0.4;
        }
    }

    /**
     * 공통 좋아요 곡 수 계산
     */
    private int calculateCommonLikedSongs(Long user1Id, Long user2Id) {
        // 간단한 쿼리 사용
        List<UserSongLike> allLikes = userSongLikeRepository.findAll();

        Set<Long> user1SongIds = allLikes.stream()
                .filter(like -> like.getUser().getId().equals(user1Id))
                .map(like -> like.getSong().getId())
                .collect(Collectors.toSet());

        Set<Long> user2SongIds = allLikes.stream()
                .filter(like -> like.getUser().getId().equals(user2Id))
                .map(like -> like.getSong().getId())
                .collect(Collectors.toSet());

        user1SongIds.retainAll(user2SongIds);
        return user1SongIds.size();
    }

    /**
     * 향상된 매칭 이유 생성 - 페스티벌과 무드 포함
     */
    private String generateEnhancedMatchReason(Long user1Id, Long user2Id, double totalSimilarity) {
        try {
            // 각 요소별 점수 계산
            double helpfulScore = calculateHelpfulSimilarity(user1Id, user2Id);
            double likedSongsScore = calculateLikedSongsSimilarity(user1Id, user2Id);
            double festivalScore = calculateFestivalSimilarity(user1Id, user2Id);
            double artistScore = calculateArtistSimilarity(user1Id, user2Id);  
            double genreScore = calculateGenreSimilarity(user1Id, user2Id);
            double moodScore = calculateMoodSimilarity(user1Id, user2Id);

            List<String> reasons = new ArrayList<>();
            
            // 임계값을 넘는 요소들만 이유에 포함 (높은 우선순위 순)
            if (likedSongsScore >= 0.4) {
                reasons.add("❤️ 공통 좋아요 음악");
            }
            if (festivalScore >= 0.4) {
                reasons.add("🎪 공통 페스티벌 경험");
            }
            if (helpfulScore >= 0.3) {
                reasons.add("🤝 비슷한 리뷰에 공감");
            }
            if (artistScore >= 0.3) {
                reasons.add("🎤 공통 아티스트 취향");
            }
            if (genreScore >= 0.3) {
                reasons.add("🎶 선호 장르 일치");
            }
            if (moodScore >= 0.4) {
                reasons.add("🌙 음악 무드 일치");
            }

            if (reasons.isEmpty()) {
                return "🎵 새로운 음악 친구와의 만남";
            }

            // 최대 3개 이유만 표시
            String reasonText = reasons.stream()
                    .limit(3)
                    .collect(Collectors.joining(", "));
            int percentage = Math.min(95, (int)(totalSimilarity * 100));
            
            return String.format("%s (%d%% 매칭)", reasonText, percentage);
        } catch (Exception e) {
            log.warn("매칭 이유 생성 실패: {}", e.getMessage());
            return String.format("🎵 음악 취향 매칭 (%d%%)", 
                    (int)(totalSimilarity * 100));
        }
    }

    /**
     * 기존 매칭 이유 생성 (하위 호환)
     */
    private String generateMatchReason(Long user1Id, Long user2Id, double similarity) {
        return generateEnhancedMatchReason(user1Id, user2Id, similarity);
    }

    /**
     * 매칭 상태 업데이트
     */
    public UserMatch updateMatchStatus(Long matchId, String status) {
        UserMatch match = userMatchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("매칭을 찾을 수 없습니다."));

        match.setMatchStatus(status);
        match.setLastInteractionAt(LocalDateTime.now());

        return userMatchRepository.save(match);
    }

    /**
     * 사용자의 모든 매칭 조회
     */
    public List<UserMatch> getUserMatches(Long userId) {
        return userMatchRepository.findMatchesByUserId(userId);
    }

    /**
     * 사용자의 상위 매칭들 조회
     */
    public List<UserMatch> getTopMatches(Long userId, int limit) {
        return userMatchRepository.findTopMatchesByUserId(userId, limit);
    }
}