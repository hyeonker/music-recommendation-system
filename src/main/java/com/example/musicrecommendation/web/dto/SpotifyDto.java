package com.example.musicrecommendation.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Spotify API 응답 DTO들
 */
public class SpotifyDto {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("expires_in")
        private Integer expiresIn;

        // Getters and Setters
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }

        public Integer getExpiresIn() { return expiresIn; }
        public void setExpiresIn(Integer expiresIn) { this.expiresIn = expiresIn; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrackSearchResponse {
        private TracksData tracks;

        public TracksData getTracks() { return tracks; }
        public void setTracks(TracksData tracks) { this.tracks = tracks; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TracksData {
        private List<Track> items;
        private Integer total;

        public List<Track> getItems() { return items; }
        public void setItems(List<Track> items) { this.items = items; }

        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Track {
        private String id;
        private String name;
        private List<Artist> artists;
        private Album album;
        private Integer popularity;

        @JsonProperty("duration_ms")
        private Integer durationMs;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public List<Artist> getArtists() { return artists; }
        public void setArtists(List<Artist> artists) { this.artists = artists; }

        public Album getAlbum() { return album; }
        public void setAlbum(Album album) { this.album = album; }

        public Integer getPopularity() { return popularity; }
        public void setPopularity(Integer popularity) { this.popularity = popularity; }

        public Integer getDurationMs() { return durationMs; }
        public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Artist {
        private String id;
        private String name;
        private List<String> genres;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public List<String> getGenres() { return genres; }
        public void setGenres(List<String> genres) { this.genres = genres; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Album {
        private String id;
        private String name;

        @JsonProperty("release_date")
        private String releaseDate;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getReleaseDate() { return releaseDate; }
        public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AudioFeatures {
        private String id;
        private Double acousticness;
        private Double danceability;
        private Double energy;
        private Double instrumentalness;
        private Double liveness;
        private Double loudness;
        private Double speechiness;
        private Double valence;
        private Double tempo;
        private Integer key;
        private Integer mode;

        @JsonProperty("time_signature")
        private Integer timeSignature;

        @JsonProperty("duration_ms")
        private Integer durationMs;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public Double getAcousticness() { return acousticness; }
        public void setAcousticness(Double acousticness) { this.acousticness = acousticness; }

        public Double getDanceability() { return danceability; }
        public void setDanceability(Double danceability) { this.danceability = danceability; }

        public Double getEnergy() { return energy; }
        public void setEnergy(Double energy) { this.energy = energy; }

        public Double getInstrumentalness() { return instrumentalness; }
        public void setInstrumentalness(Double instrumentalness) { this.instrumentalness = instrumentalness; }

        public Double getLiveness() { return liveness; }
        public void setLiveness(Double liveness) { this.liveness = liveness; }

        public Double getLoudness() { return loudness; }
        public void setLoudness(Double loudness) { this.loudness = loudness; }

        public Double getSpeechiness() { return speechiness; }
        public void setSpeechiness(Double speechiness) { this.speechiness = speechiness; }

        public Double getValence() { return valence; }
        public void setValence(Double valence) { this.valence = valence; }

        public Double getTempo() { return tempo; }
        public void setTempo(Double tempo) { this.tempo = tempo; }

        public Integer getKey() { return key; }
        public void setKey(Integer key) { this.key = key; }

        public Integer getMode() { return mode; }
        public void setMode(Integer mode) { this.mode = mode; }

        public Integer getTimeSignature() { return timeSignature; }
        public void setTimeSignature(Integer timeSignature) { this.timeSignature = timeSignature; }

        public Integer getDurationMs() { return durationMs; }
        public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }
    }
}