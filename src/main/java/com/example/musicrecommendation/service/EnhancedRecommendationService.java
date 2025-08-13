package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.*;
import com.example.musicrecommendation.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Spotify 데이터 기반 고도화된 추천 서비스
 */
@Service
@Transactional
public class EnhancedRecommendationService {

    private final SpotifyApiService spotifyApiService;
    private final SpotifySyncService spotifySyncService;
    private final UserRepository userRepository;
    private final UserSongLikeRepository userSongLikeRepository;
    private final MusicMetadataRepository musicMetadataRepository;
    private final SongRepository songRepository;

    public EnhancedRecommendationService(SpotifyApiService spotifyApiService,
                                         SpotifySyncService spotifySyncService,
                                         UserRepository userRepository,
                                         UserSongLikeRepository userSongLikeRepository,
                                         MusicMetadataRepository musicMetadataRepository,
                                         SongRepository songRepository) {
        this.spotifyApiService = spotifyApiService;
        this.spotifySyncService = spotifySyncService;
        this.userRepository = userRepository;
        this.userSongLikeRepository = userSongLikeRepository;
        this.musicMetadataRepository = musicMetadataRepository;
        this.songRepository = songRepository;
    }

    /**
     * 🎯 AI 기반 개인화 추천 (Spotify 데이터 활용)
     */
    public RecommendationResponse.RecommendationData getAIPersonalizedRecommendations(Long userId, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 1. 사용자 좋아요 곡들의 Spotify 데이터 분석
        UserMusicProfile musicProfile = analyzeUserMusicTaste(userId);

        // 2. Spotify에서 유사한 특성의 곡들 검색
        List<RecommendationResponse.RecommendedSongDto> recommendations =
                findSimilarSpotifyTracks(musicProfile, limit);

        return new RecommendationResponse.RecommendationData(
                "🤖 AI 맞춤 추천 (Spotify 분석)",
                user.getName() + "님의 음악 취향을 분석한 AI 추천",
                recommendations,
                LocalDateTime.now()
        );
    }

    /**
     * 🎵 장르 기반 스마트 추천
     */
    public RecommendationResponse.RecommendationData getGenreBasedRecommendations(String genre, int limit) {
        // Spotify에서 해당 장르의 인기 곡들 가져오기
        Optional<SpotifyDto.TrackSearchResponse> spotifyResult =
                spotifyApiService.getPopularTracks(genre, limit * 2);

        List<RecommendationResponse.RecommendedSongDto> recommendations = new ArrayList<>();

        if (spotifyResult.isPresent() && spotifyResult.get().getTracks() != null) {
            List<SpotifyDto.Track> tracks = spotifyResult.get().getTracks().getItems();

            for (int i = 0; i < Math.min(tracks.size(), limit); i++) {
                SpotifyDto.Track track = tracks.get(i);

                // 오디오 특성 분석
                Optional<SpotifyDto.AudioFeatures> audioFeatures =
                        spotifyApiService.getAudioFeatures(track.getId());

                String reason = generateSpotifyRecommendationReason(track, audioFeatures);
                double score = calculateSpotifyTrackScore(track, audioFeatures);

                recommendations.add(new RecommendationResponse.RecommendedSongDto(
                        i + 1,
                        convertSpotifyTrackToSongResponse(track),
                        score,
                        reason,
                        "🎼 " + genre.toUpperCase() + " 장르 추천"
                ));
            }
        }

        return new RecommendationResponse.RecommendationData(
                "🎼 " + genre + " 장르 스마트 추천",
                "Spotify 오디오 분석 기반 " + genre + " 추천곡",
                recommendations,
                LocalDateTime.now()
        );
    }

    /**
     * 🔥 실시간 트렌딩 추천
     */
    public RecommendationResponse.RecommendationData getTrendingRecommendations(int limit) {
        // 여러 인기 장르의 트렌딩 곡들 수집
        String[] trendingGenres = {"pop", "hip-hop", "electronic", "indie", "k-pop"};
        List<RecommendationResponse.RecommendedSongDto> allRecommendations = new ArrayList<>();

        for (String genre : trendingGenres) {
            Optional<SpotifyDto.TrackSearchResponse> result =
                    spotifyApiService.getPopularTracks(genre, 4);

            if (result.isPresent() && result.get().getTracks() != null) {
                List<SpotifyDto.Track> tracks = result.get().getTracks().getItems();

                for (SpotifyDto.Track track : tracks) {
                    if (allRecommendations.size() >= limit) break;

                    double trendScore = 0.8 + (track.getPopularity() != null ?
                            track.getPopularity() / 500.0 : 0.1);

                    allRecommendations.add(new RecommendationResponse.RecommendedSongDto(
                            allRecommendations.size() + 1,
                            convertSpotifyTrackToSongResponse(track),
                            trendScore,
                            "🔥 " + genre + " 트렌딩 인기곡",
                            "⚡ 실시간 인기"
                    ));
                }
            }
        }

        // 인기도 순으로 정렬
        allRecommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        return new RecommendationResponse.RecommendationData(
                "🔥 실시간 트렌딩 추천",
                "전 세계 실시간 인기곡 기반 추천",
                allRecommendations.stream().limit(limit).collect(Collectors.toList()),
                LocalDateTime.now()
        );
    }

    /**
     * 🎨 무드 기반 추천
     */
    public RecommendationResponse.RecommendationData getMoodBasedRecommendations(String mood, int limit) {
        Map<String, String> moodToGenre = Map.of(
                "happy", "pop",
                "chill", "indie",
                "energetic", "electronic",
                "romantic", "r&b",
                "focus", "ambient"
        );

        String genre = moodToGenre.getOrDefault(mood.toLowerCase(), "pop");
        RecommendationResponse.RecommendationData genreRecommendations =
                getGenreBasedRecommendations(genre, limit);

        // 무드에 맞게 타이틀과 설명 변경
        return new RecommendationResponse.RecommendationData(
                "🎨 " + mood.toUpperCase() + " 무드 추천",
                mood + " 감정에 완벽한 음악 추천",
                genreRecommendations.getRecommendations(),
                LocalDateTime.now()
        );
    }

    /**
     * 사용자 음악 취향 프로필 분석
     */
    private UserMusicProfile analyzeUserMusicTaste(Long userId) {
        List<UserSongLike> userLikes = userSongLikeRepository.findAll().stream()
                .filter(like -> like.getUser().getId().equals(userId))
                .collect(Collectors.toList());

        if (userLikes.isEmpty()) {
            return new UserMusicProfile(); // 기본 프로필
        }

        // 좋아한 곡들의 메타데이터 수집
        List<MusicMetadata> likedMetadata = userLikes.stream()
                .map(like -> musicMetadataRepository.findBySongId(like.getSong().getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return calculateAverageProfile(likedMetadata);
    }

    /**
     * 음악 특성 평균 계산
     */
    private UserMusicProfile calculateAverageProfile(List<MusicMetadata> metadata) {
        if (metadata.isEmpty()) {
            return new UserMusicProfile();
        }

        double avgDanceability = metadata.stream()
                .mapToDouble(m -> m.getDanceability() != null ? m.getDanceability() : 0.5)
                .average().orElse(0.5);

        double avgEnergy = metadata.stream()
                .mapToDouble(m -> m.getEnergy() != null ? m.getEnergy() : 0.5)
                .average().orElse(0.5);

        double avgValence = metadata.stream()
                .mapToDouble(m -> m.getValence() != null ? m.getValence() : 0.5)
                .average().orElse(0.5);

        return new UserMusicProfile(avgDanceability, avgEnergy, avgValence);
    }

    /**
     * 유사한 Spotify 트랙 찾기
     */
    private List<RecommendationResponse.RecommendedSongDto> findSimilarSpotifyTracks(
            UserMusicProfile profile, int limit) {

        // 사용자 프로필에 맞는 검색어 생성
        String searchQuery = generateSearchQueryFromProfile(profile);

        Optional<SpotifyDto.TrackSearchResponse> result =
                spotifyApiService.searchTracks(searchQuery, limit);

        List<RecommendationResponse.RecommendedSongDto> recommendations = new ArrayList<>();

        if (result.isPresent() && result.get().getTracks() != null) {
            List<SpotifyDto.Track> tracks = result.get().getTracks().getItems();

            for (int i = 0; i < tracks.size(); i++) {
                SpotifyDto.Track track = tracks.get(i);
                double similarity = calculateProfileSimilarity(profile, track.getId());

                recommendations.add(new RecommendationResponse.RecommendedSongDto(
                        i + 1,
                        convertSpotifyTrackToSongResponse(track),
                        similarity,
                        "🎯 AI 분석: " + Math.round(similarity * 100) + "% 일치",
                        "🤖 개인화 추천"
                ));
            }
        }

        return recommendations;
    }

    // Helper Methods
    private String generateSearchQueryFromProfile(UserMusicProfile profile) {
        if (profile.avgDanceability > 0.7) {
            return "dance pop electronic";
        } else if (profile.avgEnergy < 0.3) {
            return "ballad acoustic chill";
        } else if (profile.avgValence > 0.7) {
            return "happy pop upbeat";
        } else {
            return "popular music 2024";
        }
    }

    private double calculateProfileSimilarity(UserMusicProfile profile, String trackId) {
        Optional<SpotifyDto.AudioFeatures> features = spotifyApiService.getAudioFeatures(trackId);
        if (features.isEmpty()) {
            return 0.5;
        }

        SpotifyDto.AudioFeatures af = features.get();
        double danceabilityDiff = Math.abs((af.getDanceability() != null ? af.getDanceability() : 0.5) - profile.avgDanceability);
        double energyDiff = Math.abs((af.getEnergy() != null ? af.getEnergy() : 0.5) - profile.avgEnergy);
        double valenceDiff = Math.abs((af.getValence() != null ? af.getValence() : 0.5) - profile.avgValence);

        return 1.0 - ((danceabilityDiff + energyDiff + valenceDiff) / 3.0);
    }

    private SimpleStatsResponse.SongResponse convertSpotifyTrackToSongResponse(SpotifyDto.Track track) {
        String artist = track.getArtists() != null && !track.getArtists().isEmpty() ?
                track.getArtists().get(0).getName() : "Unknown Artist";

        return new SimpleStatsResponse.SongResponse(
                1L, // 임시 ID
                track.getName(),
                artist,
                track.getAlbum() != null ? track.getAlbum().getName() : "Unknown Album",
                0L // 임시 좋아요 수
        );
    }

    private String generateSpotifyRecommendationReason(SpotifyDto.Track track,
                                                       Optional<SpotifyDto.AudioFeatures> audioFeatures) {
        if (audioFeatures.isPresent()) {
            SpotifyDto.AudioFeatures af = audioFeatures.get();
            if (af.getDanceability() != null && af.getDanceability() > 0.8) {
                return "🕺 댄서블한 리듬 (" + Math.round(af.getDanceability() * 100) + "%)";
            } else if (af.getEnergy() != null && af.getEnergy() > 0.8) {
                return "⚡ 높은 에너지 (" + Math.round(af.getEnergy() * 100) + "%)";
            } else if (af.getValence() != null && af.getValence() > 0.8) {
                return "😊 밝고 긍정적인 분위기 (" + Math.round(af.getValence() * 100) + "%)";
            }
        }

        return "🎵 Spotify 추천 인기곡";
    }

    private double calculateSpotifyTrackScore(SpotifyDto.Track track,
                                              Optional<SpotifyDto.AudioFeatures> audioFeatures) {
        double baseScore = 0.7;

        if (track.getPopularity() != null) {
            baseScore += track.getPopularity() / 200.0; // 0.5 최대 추가
        }

        if (audioFeatures.isPresent()) {
            SpotifyDto.AudioFeatures af = audioFeatures.get();
            if (af.getDanceability() != null && af.getEnergy() != null) {
                baseScore += (af.getDanceability() + af.getEnergy()) / 10.0; // 0.2 최대 추가
            }
        }

        return Math.min(1.0, baseScore);
    }

    // 사용자 음악 프로필 내부 클래스
    private static class UserMusicProfile {
        double avgDanceability = 0.5;
        double avgEnergy = 0.5;
        double avgValence = 0.5;

        public UserMusicProfile() {}

        public UserMusicProfile(double danceability, double energy, double valence) {
            this.avgDanceability = danceability;
            this.avgEnergy = energy;
            this.avgValence = valence;
        }
    }
}