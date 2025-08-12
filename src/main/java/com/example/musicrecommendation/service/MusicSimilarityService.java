package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.*;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 음악 유사도 계산 서비스
 */
@Service
public class MusicSimilarityService {

    private final EuclideanDistance euclideanDistance;
    private final PearsonsCorrelation pearsonsCorrelation;

    public MusicSimilarityService() {
        this.euclideanDistance = new EuclideanDistance();
        this.pearsonsCorrelation = new PearsonsCorrelation();
    }

    /**
     * 두 곡의 유사도 계산 (0.0 ~ 1.0, 높을수록 유사)
     */
    public double calculateSimilarity(MusicMetadata song1, MusicMetadata song2) {
        if (song1 == null || song2 == null) return 0.0;

        // 특성 벡터 추출
        double[] vector1 = song1.getFeatureVector();
        double[] vector2 = song2.getFeatureVector();

        // 유클리드 거리 계산 (0에 가까울수록 유사)
        double distance = euclideanDistance.compute(vector1, vector2);

        // 거리를 유사도로 변환 (0~1 범위)
        // 최대 거리는 sqrt(8) ≈ 2.83 (8차원에서 각 축이 1.0 차이날 때)
        double maxDistance = Math.sqrt(vector1.length);
        double similarity = 1.0 - (distance / maxDistance);

        return Math.max(0.0, Math.min(1.0, similarity));
    }

    /**
     * 특정 곡과 유사한 곡들 찾기
     */
    public List<SimilarSong> findSimilarSongs(MusicMetadata targetSong,
                                              List<MusicMetadata> candidateSongs,
                                              int limit) {

        return candidateSongs.stream()
                .filter(song -> !song.getId().equals(targetSong.getId())) // 자기 자신 제외
                .map(song -> new SimilarSong(song, calculateSimilarity(targetSong, song)))
                .filter(similarSong -> similarSong.similarity > 0.3) // 최소 유사도 필터
                .sorted((a, b) -> Double.compare(b.similarity, a.similarity)) // 유사도 높은 순
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 취향과 유사한 곡들 찾기
     */
    public List<SimilarSong> findSongsMatchingPreference(UserPreferenceProfile userProfile,
                                                         List<MusicMetadata> candidateSongs,
                                                         int limit) {

        double[] userPreferenceVector = userProfile.getPreferenceVector();

        return candidateSongs.stream()
                .map(song -> {
                    double[] songVector = song.getFeatureVector();
                    double distance = euclideanDistance.compute(userPreferenceVector, songVector);
                    double maxDistance = Math.sqrt(userPreferenceVector.length);
                    double similarity = 1.0 - (distance / maxDistance);
                    return new SimilarSong(song, Math.max(0.0, Math.min(1.0, similarity)));
                })
                .filter(similarSong -> similarSong.similarity > 0.4) // 사용자 취향은 더 높은 기준
                .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 두 사용자의 음악 취향 유사도 계산
     */
    public double calculateUserSimilarity(UserPreferenceProfile user1, UserPreferenceProfile user2) {
        if (user1 == null || user2 == null) return 0.0;

        double[] vector1 = user1.getPreferenceVector();
        double[] vector2 = user2.getPreferenceVector();

        try {
            // 피어슨 상관계수 사용 (-1 ~ 1)
            double correlation = pearsonsCorrelation.correlation(vector1, vector2);

            // -1~1을 0~1로 변환
            return (correlation + 1.0) / 2.0;
        } catch (Exception e) {
            // 상관계수 계산 실패시 유클리드 거리 사용
            double distance = euclideanDistance.compute(vector1, vector2);
            double maxDistance = Math.sqrt(vector1.length);
            return 1.0 - (distance / maxDistance);
        }
    }

    /**
     * 장르 기반 유사도 계산
     */
    public double calculateGenreSimilarity(MusicMetadata song1, MusicMetadata song2) {
        String genres1 = song1.getGenres();
        String genres2 = song2.getGenres();

        if (genres1 == null || genres2 == null) return 0.0;

        // 간단한 문자열 매칭으로 장르 유사도 계산
        Set<String> genreSet1 = Set.of(genres1.toLowerCase().split(","));
        Set<String> genreSet2 = Set.of(genres2.toLowerCase().split(","));

        Set<String> intersection = new HashSet<>(genreSet1);
        intersection.retainAll(genreSet2);

        Set<String> union = new HashSet<>(genreSet1);
        union.addAll(genreSet2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * 복합 유사도 계산 (음악 특성 + 장르)
     */
    public double calculateCompositeSimilarity(MusicMetadata song1, MusicMetadata song2) {
        double featureSimilarity = calculateSimilarity(song1, song2);
        double genreSimilarity = calculateGenreSimilarity(song1, song2);

        // 가중 평균 (특성 70%, 장르 30%)
        return featureSimilarity * 0.7 + genreSimilarity * 0.3;
    }

    /**
     * 음악 클러스터링 (비슷한 특성의 곡들 그룹화)
     */
    public Map<String, List<MusicMetadata>> clusterSongs(List<MusicMetadata> songs, int clusterCount) {
        Map<String, List<MusicMetadata>> clusters = new HashMap<>();

        // 간단한 K-means 스타일 클러스터링
        for (MusicMetadata song : songs) {
            String clusterKey = determineCluster(song);
            clusters.computeIfAbsent(clusterKey, k -> new ArrayList<>()).add(song);
        }

        return clusters;
    }

    /**
     * 곡의 클러스터 결정 (음악 특성 기반)
     */
    private String determineCluster(MusicMetadata song) {
        StringBuilder cluster = new StringBuilder();

        Float energy = song.getEnergy();
        Float danceability = song.getDanceability();
        Float valence = song.getValence();
        Float acousticness = song.getAcousticness();

        // 에너지 수준
        if (energy != null) {
            if (energy > 0.7) cluster.append("High");
            else if (energy > 0.4) cluster.append("Mid");
            else cluster.append("Low");
        } else {
            cluster.append("Unknown");
        }

        cluster.append("Energy_");

        // 장르 스타일
        if (danceability != null && danceability > 0.7) {
            cluster.append("Dance");
        } else if (acousticness != null && acousticness > 0.7) {
            cluster.append("Acoustic");
        } else if (valence != null && valence > 0.7) {
            cluster.append("Upbeat");
        } else if (valence != null && valence < 0.3) {
            cluster.append("Mellow");
        } else {
            cluster.append("Balanced");
        }

        return cluster.toString();
    }

    /**
     * 유사 곡 정보 클래스
     */
    public static class SimilarSong {
        public final MusicMetadata song;
        public final double similarity;
        public final String reason;

        public SimilarSong(MusicMetadata song, double similarity) {
            this.song = song;
            this.similarity = similarity;
            this.reason = generateSimilarityReason(similarity);
        }

        private String generateSimilarityReason(double similarity) {
            if (similarity > 0.9) return "🎯 완벽한 매치";
            if (similarity > 0.8) return "🔥 매우 유사";
            if (similarity > 0.7) return "✨ 유사한 느낌";
            if (similarity > 0.6) return "👍 비슷한 스타일";
            if (similarity > 0.5) return "🎵 어느정도 유사";
            return "🤔 약간 유사";
        }
    }

    /**
     * 사용자 유사도 정보 클래스
     */
    public static class SimilarUser {
        public final UserPreferenceProfile user;
        public final double similarity;
        public final String reason;

        public SimilarUser(UserPreferenceProfile user, double similarity) {
            this.user = user;
            this.similarity = similarity;
            this.reason = generateUserSimilarityReason(similarity);
        }

        private String generateUserSimilarityReason(double similarity) {
            if (similarity > 0.9) return "🎭 취향이 거의 동일";
            if (similarity > 0.8) return "🎵 매우 비슷한 취향";
            if (similarity > 0.7) return "✨ 유사한 음악 취향";
            if (similarity > 0.6) return "👥 어느정도 비슷한 취향";
            return "🤷 약간 다른 취향";
        }
    }
}