package com.example.musicrecommendation.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 음악 메타데이터 엔티티 - Spotify API에서 가져온 상세 정보
 */
@Entity
@Table(name = "music_metadata")
public class MusicMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id")
    private Song song;

    @Column(name = "spotify_id")
    private String spotifyId;

    // === 🎵 음악 특성 데이터 ===

    @Column(name = "acousticness")
    private Float acousticness; // 어쿠스틱 정도 (0.0 ~ 1.0)

    @Column(name = "danceability")
    private Float danceability; // 댄스 가능성 (0.0 ~ 1.0)

    @Column(name = "energy")
    private Float energy; // 에너지 (0.0 ~ 1.0)

    @Column(name = "instrumentalness")
    private Float instrumentalness; // 보컬 없는 정도 (0.0 ~ 1.0)

    @Column(name = "liveness")
    private Float liveness; // 라이브 녹음 정도 (0.0 ~ 1.0)

    @Column(name = "loudness")
    private Float loudness; // 음량 (dB)

    @Column(name = "speechiness")
    private Float speechiness; // 말하기 정도 (0.0 ~ 1.0)

    @Column(name = "valence")
    private Float valence; // 긍정성 (0.0 ~ 1.0)

    @Column(name = "tempo")
    private Float tempo; // 템포 (BPM)

    // === 🎭 음악 정보 ===

    @Column(name = "key_signature")
    private Integer keySignature; // 키 (0 = C, 1 = C#, ...)

    @Column(name = "mode")
    private Integer mode; // 모드 (0 = minor, 1 = major)

    @Column(name = "time_signature")
    private Integer timeSignature; // 박자 (3, 4, 5, 6, 7)

    @Column(name = "duration_ms")
    private Integer durationMs; // 곡 길이 (밀리초)

    // === 🏷️ 장르 및 메타데이터 ===

    @Column(name = "genres", length = 1000)
    private String genres; // JSON 배열 형태로 저장

    @Column(name = "popularity")
    private Integer popularity; // Spotify 인기도 (0-100)

    @Column(name = "preview_url")
    private String previewUrl; // 30초 미리듣기 URL

    @Column(name = "external_urls", length = 500)
    private String externalUrls; // Spotify 외부 URL들

    // === 📅 메타데이터 ===

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // === 생성자 ===

    public MusicMetadata() {
        this.fetchedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public MusicMetadata(Song song, String spotifyId) {
        this();
        this.song = song;
        this.spotifyId = spotifyId;
    }

    // === 🎯 유틸리티 메서드들 ===

    /**
     * 음악 특성 벡터 생성 (유사도 계산용)
     */
    public double[] getFeatureVector() {
        return new double[] {
                acousticness != null ? acousticness : 0.5,
                danceability != null ? danceability : 0.5,
                energy != null ? energy : 0.5,
                instrumentalness != null ? instrumentalness : 0.5,
                liveness != null ? liveness : 0.5,
                speechiness != null ? speechiness : 0.5,
                valence != null ? valence : 0.5,
                tempo != null ? (tempo / 200.0) : 0.5 // 0-200 BPM을 0-1로 정규화
        };
    }

    /**
     * 음악 스타일 설명 생성
     */
    public String getStyleDescription() {
        StringBuilder style = new StringBuilder();

        if (energy != null && energy > 0.7) style.append("에너지틱한 ");
        if (danceability != null && danceability > 0.7) style.append("댄서블한 ");
        if (acousticness != null && acousticness > 0.7) style.append("어쿠스틱한 ");
        if (valence != null && valence > 0.7) style.append("밝은 ");
        else if (valence != null && valence < 0.3) style.append("감성적인 ");

        if (style.length() == 0) style.append("균형잡힌 ");

        style.append("음악");
        return style.toString();
    }

    /**
     * 메타데이터 업데이트
     */
    public void updateMetadata() {
        this.updatedAt = LocalDateTime.now();
    }

    // === Getters and Setters ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Song getSong() { return song; }
    public void setSong(Song song) { this.song = song; }

    public String getSpotifyId() { return spotifyId; }
    public void setSpotifyId(String spotifyId) { this.spotifyId = spotifyId; }

    public Float getAcousticness() { return acousticness; }
    public void setAcousticness(Float acousticness) { this.acousticness = acousticness; }

    public Float getDanceability() { return danceability; }
    public void setDanceability(Float danceability) { this.danceability = danceability; }

    public Float getEnergy() { return energy; }
    public void setEnergy(Float energy) { this.energy = energy; }

    public Float getInstrumentalness() { return instrumentalness; }
    public void setInstrumentalness(Float instrumentalness) { this.instrumentalness = instrumentalness; }

    public Float getLiveness() { return liveness; }
    public void setLiveness(Float liveness) { this.liveness = liveness; }

    public Float getLoudness() { return loudness; }
    public void setLoudness(Float loudness) { this.loudness = loudness; }

    public Float getSpeechiness() { return speechiness; }
    public void setSpeechiness(Float speechiness) { this.speechiness = speechiness; }

    public Float getValence() { return valence; }
    public void setValence(Float valence) { this.valence = valence; }

    public Float getTempo() { return tempo; }
    public void setTempo(Float tempo) { this.tempo = tempo; }

    public Integer getKeySignature() { return keySignature; }
    public void setKeySignature(Integer keySignature) { this.keySignature = keySignature; }

    public Integer getMode() { return mode; }
    public void setMode(Integer mode) { this.mode = mode; }

    public Integer getTimeSignature() { return timeSignature; }
    public void setTimeSignature(Integer timeSignature) { this.timeSignature = timeSignature; }

    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }

    public String getGenres() { return genres; }
    public void setGenres(String genres) { this.genres = genres; }

    public Integer getPopularity() { return popularity; }
    public void setPopularity(Integer popularity) { this.popularity = popularity; }

    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }

    public String getExternalUrls() { return externalUrls; }
    public void setExternalUrls(String externalUrls) { this.externalUrls = externalUrls; }

    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(LocalDateTime fetchedAt) { this.fetchedAt = fetchedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
