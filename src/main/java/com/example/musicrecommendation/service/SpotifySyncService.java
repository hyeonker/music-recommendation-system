package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.*;
import com.example.musicrecommendation.web.dto.spotify.SpotifyDto; // ★ 경로 수정!
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spotify 데이터 동기화 서비스
 */
@Service
@Transactional
public class SpotifySyncService {

    private final SpotifyApiService spotifyApiService;
    private final SongRepository songRepository;
    private final MusicMetadataRepository musicMetadataRepository;

    public SpotifySyncService(SpotifyApiService spotifyApiService,
                              SongRepository songRepository,
                              MusicMetadataRepository musicMetadataRepository) {
        this.spotifyApiService = spotifyApiService;
        this.songRepository = songRepository;
        this.musicMetadataRepository = musicMetadataRepository;
    }

    /** Spotify에서 곡 검색하고 DB에 저장 */
    public Song syncTrackFromSpotify(String query) {
        Optional<SpotifyDto.TrackSearchResponse> searchResult = spotifyApiService.searchTracks(query, 1);

        if (searchResult.isPresent() && searchResult.get().getTracks() != null &&
                !searchResult.get().getTracks().getItems().isEmpty()) {

            SpotifyDto.Track spotifyTrack = searchResult.get().getTracks().getItems().get(0);
            return createOrUpdateSong(spotifyTrack);
        }
        return null;
    }

    /** 인기 곡들을 Spotify에서 가져와서 DB 업데이트 */
    public List<Song> syncPopularTracks(String genre, int limit) {
        Optional<SpotifyDto.TrackSearchResponse> result = spotifyApiService.getPopularTracks(genre, limit);

        if (result.isPresent() && result.get().getTracks() != null) {
            return result.get().getTracks().getItems().stream()
                    .map(this::createOrUpdateSong)
                    .filter(song -> song != null)
                    .toList();
        }
        return List.of();
    }

    /** 기존 곡의 Spotify 메타데이터 업데이트 */
    public boolean updateMusicMetadata(Long songId, String spotifyId) {
        Optional<Song> songOpt = songRepository.findById(songId);
        if (songOpt.isEmpty()) return false;

        Optional<SpotifyDto.AudioFeatures> audioFeatures = spotifyApiService.getAudioFeatures(spotifyId);
        if (audioFeatures.isEmpty()) return false;

        Song song = songOpt.get();
        SpotifyDto.AudioFeatures features = audioFeatures.get();

        // 기존 메타데이터 찾기 또는 새로 생성
        Optional<MusicMetadata> existingMetadata = musicMetadataRepository.findBySongId(songId);
        MusicMetadata metadata = existingMetadata.orElse(new MusicMetadata());

        // Spotify 데이터로 업데이트 (Double → Float 변환)
        metadata.setSong(song);
        metadata.setSpotifyId(spotifyId);
        if (features.getAcousticness() != null) metadata.setAcousticness(features.getAcousticness().floatValue());
        if (features.getDanceability() != null) metadata.setDanceability(features.getDanceability().floatValue());
        if (features.getEnergy() != null) metadata.setEnergy(features.getEnergy().floatValue());
        if (features.getInstrumentalness() != null) metadata.setInstrumentalness(features.getInstrumentalness().floatValue());
        if (features.getLiveness() != null) metadata.setLiveness(features.getLiveness().floatValue());
        if (features.getLoudness() != null) metadata.setLoudness(features.getLoudness().floatValue());
        if (features.getSpeechiness() != null) metadata.setSpeechiness(features.getSpeechiness().floatValue());
        if (features.getValence() != null) metadata.setValence(features.getValence().floatValue());
        if (features.getTempo() != null) metadata.setTempo(features.getTempo().floatValue());
        if (features.getKey() != null) metadata.setKeySignature(features.getKey());
        if (features.getMode() != null) metadata.setMode(features.getMode());
        if (features.getTimeSignature() != null) metadata.setTimeSignature(features.getTimeSignature());
        if (features.getDurationMs() != null) metadata.setDurationMs(features.getDurationMs());
        metadata.setUpdatedAt(LocalDateTime.now());

        musicMetadataRepository.save(metadata);
        return true;
    }

    /** Spotify Track을 Song 엔티티로 변환/저장 (간단화) */
    private Song createOrUpdateSong(SpotifyDto.Track spotifyTrack) {
        try {
            // TODO: 실제 매핑/저장을 구현. 현재는 간단히 샘플로 첫 곡을 반환.
            List<Song> allSongs = songRepository.findAll();
            if (!allSongs.isEmpty()) return allSongs.get(0);
            return null;
        } catch (Exception e) {
            System.err.println("Spotify 곡 저장 실패: " + e.getMessage());
            return null;
        }
    }

    /** 동기화 상태 확인 */
    public Object getSyncStatus() {
        long totalSongs = songRepository.count();
        long songsWithMetadata = musicMetadataRepository.count();
        boolean spotifyConnected = spotifyApiService.testConnection();

        return new Object() {
            public final boolean spotifyConnected = SpotifySyncService.this.spotifyApiService.testConnection();
            public final long totalSongs = SpotifySyncService.this.songRepository.count();
            public final long songsWithSpotifyData = SpotifySyncService.this.musicMetadataRepository.count();
            public final double syncPercentage = SpotifySyncService.this.songRepository.count() > 0 ?
                    Math.round((double) SpotifySyncService.this.musicMetadataRepository.count() / SpotifySyncService.this.songRepository.count() * 100.0) : 0.0;
            public final String status = SpotifySyncService.this.spotifyApiService.testConnection() ?
                    "🟢 Spotify 연동 활성화" : "🔴 Spotify 연동 비활성화";
            public final String[] capabilities = SpotifySyncService.this.spotifyApiService.testConnection() ? new String[]{
                    "🔍 실시간 곡 검색",
                    "📊 오디오 특성 분석",
                    "🎵 인기 곡 데이터 동기화",
                    "🎼 고도화된 추천 알고리즘"
            } : new String[]{
                    "❌ Spotify API 연결 필요",
                    "⚙️ Client ID/Secret 설정 확인"
            };
        };
    }
}
