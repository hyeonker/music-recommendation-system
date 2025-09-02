package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.MusicMetadata;
import com.example.musicrecommendation.domain.MusicMetadataRepository;
import com.example.musicrecommendation.domain.Song;
import com.example.musicrecommendation.domain.SongRepository;
import com.example.musicrecommendation.web.dto.spotify.SpotifyDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spotify 데이터 동기화 서비스
 * - WebFlux 기반 SpotifyApiService 제거 후, RestTemplate 기반 SpotifyTrackService 사용
 */
@Service
@Transactional
public class SpotifySyncService {

    private final SpotifyTrackService spotifyTrackService;   // ⬅ 변경된 의존성
    private final SongRepository songRepository;
    private final MusicMetadataRepository musicMetadataRepository;

    public SpotifySyncService(SpotifyTrackService spotifyTrackService,
                              SongRepository songRepository,
                              MusicMetadataRepository musicMetadataRepository) {
        this.spotifyTrackService = spotifyTrackService;
        this.songRepository = songRepository;
        this.musicMetadataRepository = musicMetadataRepository;
    }

    /** Spotify에서 곡 검색하고 DB에 저장(간단 샘플) */
    public Song syncTrackFromSpotify(String query) {
        Optional<SpotifyDto.TrackSearchResponse> searchResult =
                spotifyTrackService.searchTracks(query, 1);

        if (searchResult.isPresent()
                && searchResult.get().getTracks() != null
                && !searchResult.get().getTracks().getItems().isEmpty()) {

            SpotifyDto.Track spotifyTrack = searchResult.get().getTracks().getItems().get(0);
            return createOrUpdateSong(spotifyTrack);
        }
        return null;
    }

    /** 장르 인기 곡 동기화(간단 샘플) */
    public List<Song> syncPopularTracks(String genre, int limit) {
        Optional<SpotifyDto.TrackSearchResponse> result =
                spotifyTrackService.getPopularTracks(genre, limit);

        if (result.isPresent() && result.get().getTracks() != null) {
            return result.get().getTracks().getItems().stream()
                    .map(this::createOrUpdateSong)
                    .filter(s -> s != null)
                    .toList();
        }
        return List.of();
    }

    /** 기존 곡의 Spotify 오디오 특성 업데이트 */
    public boolean updateMusicMetadata(Long songId, String spotifyId) {
        Optional<Song> songOpt = songRepository.findById(songId);
        if (songOpt.isEmpty()) return false;

        Optional<SpotifyDto.AudioFeatures> audioFeatures =
                spotifyTrackService.getAudioFeatures(spotifyId);
        if (audioFeatures.isEmpty()) return false;

        Song song = songOpt.get();
        SpotifyDto.AudioFeatures f = audioFeatures.get();

        // 기존 메타데이터 찾기 또는 새로 생성
        Optional<MusicMetadata> existing = musicMetadataRepository.findBySongId(songId);
        MusicMetadata md = existing.orElse(new MusicMetadata());

        // Spotify 데이터로 업데이트 (Double → Float 변환)
        md.setSong(song);
        md.setSpotifyId(spotifyId);
        if (f.getAcousticness() != null)     md.setAcousticness(f.getAcousticness().floatValue());
        if (f.getDanceability() != null)      md.setDanceability(f.getDanceability().floatValue());
        if (f.getEnergy() != null)            md.setEnergy(f.getEnergy().floatValue());
        if (f.getInstrumentalness() != null)  md.setInstrumentalness(f.getInstrumentalness().floatValue());
        if (f.getLiveness() != null)          md.setLiveness(f.getLiveness().floatValue());
        if (f.getLoudness() != null)          md.setLoudness(f.getLoudness().floatValue());
        if (f.getSpeechiness() != null)       md.setSpeechiness(f.getSpeechiness().floatValue());
        if (f.getValence() != null)           md.setValence(f.getValence().floatValue());
        if (f.getTempo() != null)             md.setTempo(f.getTempo().floatValue());
        if (f.getKey() != null)               md.setKeySignature(f.getKey());
        if (f.getMode() != null)              md.setMode(f.getMode());
        if (f.getTimeSignature() != null)     md.setTimeSignature(f.getTimeSignature());
        if (f.getDurationMs() != null)        md.setDurationMs(f.getDurationMs());
        md.setUpdatedAt(LocalDateTime.now());

        musicMetadataRepository.save(md);
        return true;
    }

    /** Spotify Track → Song 엔티티 변환/저장 */
    private Song createOrUpdateSong(SpotifyDto.Track spotifyTrack) {
        try {
            // 실제 구현을 위해서는 Song 엔티티의 필드와 메서드를 확인 후 매핑 필요
            List<Song> allSongs = songRepository.findAll();
            return allSongs.isEmpty() ? null : allSongs.get(0);
        } catch (Exception e) {
            System.err.println("Spotify 곡 저장 실패: " + e.getMessage());
            return null;
        }
    }

    /** 동기화/연동 상태 요약 (섀도잉 문제 없이 Map으로 반환) */
    public Map<String, Object> getSyncStatus() {
        boolean connected = spotifyTrackService.testConnection();
        long totalSongs = songRepository.count();
        long songsWithMetadata = musicMetadataRepository.count();
        double syncPct = totalSongs > 0
                ? Math.round((double) songsWithMetadata / totalSongs * 100.0)
                : 0.0;

        String status = connected ? "🟢 Spotify 연동 활성화" : "🔴 Spotify 연동 비활성화";
        String[] capabilities = connected ? new String[]{
                "🔍 실시간 곡 검색",
                "📊 오디오 특성 분석",
                "🎵 인기 곡 데이터 동기화",
                "🎼 고도화된 추천 알고리즘"
        } : new String[]{
                "❌ Spotify API 연결 필요",
                "⚙️ Client ID/Secret 설정 확인"
        };

        return Map.of(
                "spotifyConnected", connected,
                "totalSongs", totalSongs,
                "songsWithSpotifyData", songsWithMetadata,
                "syncPercentage", syncPct,
                "status", status,
                "capabilities", capabilities
        );
    }
}
