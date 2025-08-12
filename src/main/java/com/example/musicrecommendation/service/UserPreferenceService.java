package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 사용자 취향 분석 서비스
 */
@Service
@Transactional
public class UserPreferenceService {

    private final MusicMetadataRepository musicMetadataRepository;
    private final UserSongLikeRepository userSongLikeRepository;
    private final UserRepository userRepository;

    public UserPreferenceService(MusicMetadataRepository musicMetadataRepository,
                                 UserSongLikeRepository userSongLikeRepository,
                                 UserRepository userRepository) {
        this.musicMetadataRepository = musicMetadataRepository;
        this.userSongLikeRepository = userSongLikeRepository;
        this.userRepository = userRepository;
    }

    /**
     * 사용자 취향 프로필 생성/업데이트
     */
    public UserPreferenceProfile createOrUpdateUserProfile(Long userId) {
        // 사용자가 좋아한 곡들의 메타데이터 조회
        List<MusicMetadata> likedSongsMetadata = musicMetadataRepository.findLikedSongMetadataByUserId(userId);

        if (likedSongsMetadata.isEmpty()) {
            return createDefaultProfile(userId);
        }

        // 평균 특성 계산
        UserPreferenceProfile profile = new UserPreferenceProfile();
        profile.setUser(userRepository.findById(userId).orElse(null));

        calculateAverageFeatures(profile, likedSongsMetadata);
        calculatePreferredGenresAndArtists(profile, likedSongsMetadata);
        calculateActivityPatterns(profile, userId);
        calculateRecommendationWeights(profile, likedSongsMetadata);

        profile.calculateDiversityScore();
        profile.updateTimestamp();

        return profile;
    }

    /**
     * 평균 음악 특성 계산
     */
    private void calculateAverageFeatures(UserPreferenceProfile profile, List<MusicMetadata> likedSongs) {
        if (likedSongs.isEmpty()) return;

        double avgAcousticness = 0, avgDanceability = 0, avgEnergy = 0, avgInstrumentalness = 0;
        double avgLiveness = 0, avgSpeechiness = 0, avgValence = 0, avgTempo = 0;
        int count = 0;

        for (MusicMetadata metadata : likedSongs) {
            if (metadata.getAcousticness() != null) avgAcousticness += metadata.getAcousticness();
            if (metadata.getDanceability() != null) avgDanceability += metadata.getDanceability();
            if (metadata.getEnergy() != null) avgEnergy += metadata.getEnergy();
            if (metadata.getInstrumentalness() != null) avgInstrumentalness += metadata.getInstrumentalness();
            if (metadata.getLiveness() != null) avgLiveness += metadata.getLiveness();
            if (metadata.getSpeechiness() != null) avgSpeechiness += metadata.getSpeechiness();
            if (metadata.getValence() != null) avgValence += metadata.getValence();
            if (metadata.getTempo() != null) avgTempo += metadata.getTempo();
            count++;
        }

        if (count > 0) {
            profile.setAvgAcousticness((float) (avgAcousticness / count));
            profile.setAvgDanceability((float) (avgDanceability / count));
            profile.setAvgEnergy((float) (avgEnergy / count));
            profile.setAvgInstrumentalness((float) (avgInstrumentalness / count));
            profile.setAvgLiveness((float) (avgLiveness / count));
            profile.setAvgSpeechiness((float) (avgSpeechiness / count));
            profile.setAvgValence((float) (avgValence / count));
            profile.setAvgTempo((float) (avgTempo / count));
        }
    }

    /**
     * 선호 장르 및 아티스트 분석
     */
    private void calculatePreferredGenresAndArtists(UserPreferenceProfile profile, List<MusicMetadata> likedSongs) {
        // 장르 빈도 계산
        Map<String, Integer> genreCount = new HashMap<>();
        Map<String, Integer> artistCount = new HashMap<>();

        for (MusicMetadata metadata : likedSongs) {
            // 장르 분석
            if (metadata.getGenres() != null) {
                String[] genres = metadata.getGenres().split(",");
                for (String genre : genres) {
                    String cleanGenre = genre.trim();
                    genreCount.merge(cleanGenre, 1, Integer::sum);
                }
            }

            // 아티스트 분석
            if (metadata.getSong() != null && metadata.getSong().getArtist() != null) {
                String artist = metadata.getSong().getArtist();
                artistCount.merge(artist, 1, Integer::sum);
            }
        }

        // 상위 장르/아티스트를 JSON 형태로 저장
        List<String> topGenres = genreCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<String> topArtists = artistCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        profile.setPreferredGenres(String.join(",", topGenres));
        profile.setPreferredArtists(String.join(",", topArtists));
    }

    /**
     * 활동 패턴 분석
     */
    private void calculateActivityPatterns(UserPreferenceProfile profile, Long userId) {
        // 총 좋아요 수
        long totalLikes = userSongLikeRepository.countByUserId(userId);
        profile.setTotalLikes((int) totalLikes);

        // 최근 30일 활동 분석
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);

        // 간단한 평균 계산 (실제로는 더 복잡한 쿼리 필요)
        profile.setAvgLikesPerDay((float) totalLikes / 30.0f);

        // 기본값 설정 (실제로는 시간대별 분석 필요)
        profile.setMostActiveHour(20); // 저녁 8시
        profile.setMostActiveDay(6);   // 토요일
    }

    /**
     * 추천 가중치 계산
     */
    private void calculateRecommendationWeights(UserPreferenceProfile profile, List<MusicMetadata> likedSongs) {
        if (likedSongs.isEmpty()) {
            // 기본 가중치
            profile.setPopularityWeight(0.3f);
            profile.setDiscoveryWeight(0.3f);
            profile.setSimilarityWeight(0.4f);
            return;
        }

        // 사용자의 인기곡 선호도 분석
        double avgPopularity = likedSongs.stream()
                .filter(m -> m.getPopularity() != null)
                .mapToInt(MusicMetadata::getPopularity)
                .average()
                .orElse(50.0);

        // 인기도가 높은 곡을 선호할수록 popularityWeight 증가
        float popularityWeight = (float) (avgPopularity / 100.0 * 0.5); // 0~0.5
        float discoveryWeight = 1.0f - popularityWeight; // 나머지
        float similarityWeight = 0.4f; // 고정

        // 정규화
        float total = popularityWeight + discoveryWeight + similarityWeight;
        profile.setPopularityWeight(popularityWeight / total);
        profile.setDiscoveryWeight(discoveryWeight / total);
        profile.setSimilarityWeight(similarityWeight / total);
    }

    /**
     * 기본 프로필 생성 (좋아한 곡이 없는 경우)
     */
    private UserPreferenceProfile createDefaultProfile(Long userId) {
        UserPreferenceProfile profile = new UserPreferenceProfile();
        profile.setUser(userRepository.findById(userId).orElse(null));

        // 중간값으로 초기화
        profile.setAvgAcousticness(0.5f);
        profile.setAvgDanceability(0.5f);
        profile.setAvgEnergy(0.5f);
        profile.setAvgInstrumentalness(0.5f);
        profile.setAvgLiveness(0.5f);
        profile.setAvgSpeechiness(0.5f);
        profile.setAvgValence(0.5f);
        profile.setAvgTempo(120.0f);

        profile.setTotalLikes(0);
        profile.setAvgLikesPerDay(0.0f);
        profile.setListeningDiversityScore(0.5f);

        profile.updateTimestamp();
        return profile;
    }

    /**
     * 사용자 취향 요약 생성
     */
    public UserTasteSummary generateTasteSummary(UserPreferenceProfile profile) {
        if (profile == null) return new UserTasteSummary("신규 사용자", "아직 취향 분석 데이터가 없습니다.", new ArrayList<>());

        String mainCharacteristic = determineMainCharacteristic(profile);
        String description = profile.getPreferenceDescription();
        List<String> tags = generateTasteTags(profile);

        return new UserTasteSummary(mainCharacteristic, description, tags);
    }

    /**
     * 주요 특성 결정
     */
    private String determineMainCharacteristic(UserPreferenceProfile profile) {
        Float energy = profile.getAvgEnergy();
        Float danceability = profile.getAvgDanceability();
        Float valence = profile.getAvgValence();
        Float acousticness = profile.getAvgAcousticness();

        if (energy != null && energy > 0.8) return "🔥 에너지 마니아";
        if (danceability != null && danceability > 0.8) return "💃 댄스 러버";
        if (acousticness != null && acousticness > 0.8) return "🎸 어쿠스틱 애호가";
        if (valence != null && valence > 0.8) return "😊 긍정 에너지";
        if (valence != null && valence < 0.3) return "🎭 감성 리스너";

        return "🎵 올라운드 리스너";
    }

    /**
     * 취향 태그 생성
     */
    private List<String> generateTasteTags(UserPreferenceProfile profile) {
        List<String> tags = new ArrayList<>();

        Float energy = profile.getAvgEnergy();
        Float danceability = profile.getAvgDanceability();
        Float valence = profile.getAvgValence();
        Float acousticness = profile.getAvgAcousticness();
        Float tempo = profile.getAvgTempo();

        if (energy != null && energy > 0.7) tags.add("고에너지");
        if (danceability != null && danceability > 0.7) tags.add("댄서블");
        if (valence != null && valence > 0.7) tags.add("밝은음악");
        if (acousticness != null && acousticness > 0.7) tags.add("어쿠스틱");
        if (tempo != null && tempo > 140) tags.add("빠른템포");
        else if (tempo != null && tempo < 80) tags.add("느린템포");

        if (profile.getListeningDiversityScore() != null && profile.getListeningDiversityScore() > 0.7) {
            tags.add("다양한취향");
        }

        if (tags.isEmpty()) tags.add("균형잡힌취향");

        return tags;
    }

    /**
     * 사용자 취향 요약 클래스
     */
    public static class UserTasteSummary {
        public final String mainCharacteristic;
        public final String description;
        public final List<String> tags;

        public UserTasteSummary(String mainCharacteristic, String description, List<String> tags) {
            this.mainCharacteristic = mainCharacteristic;
            this.description = description;
            this.tags = tags;
        }
    }
}