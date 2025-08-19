package com.example.musicrecommendation.web.dto.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

public class SpotifyDto {

    @Data
    public static class TrackSearchResponse {
        private Tracks tracks;
    }

    @Data
    public static class Tracks {
        private List<Track> items;
    }

    @Data
    public static class Track {
        private String id;
        private String name;
        private List<Artist> artists;
        private Album album;

        @JsonProperty("duration_ms")
        private Integer durationMs;           // ← getDurationMs()

        private String uri;

        @JsonProperty("external_urls")
        private ExternalUrls externalUrls;
    }

    @Data
    public static class Artist {
        private String id;
        private String name;

        @JsonProperty("external_urls")
        private ExternalUrls externalUrls;
    }

    @Data
    public static class Album {
        private String id;
        private String name;
        private List<Image> images;
    }

    @Data
    public static class Image {
        private Integer width;
        private Integer height;
        private String url;
    }

    @Data
    public static class ExternalUrls {
        private String spotify;
    }

    @Data
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
        private Integer timeSignature;        // ← getTimeSignature()

        @JsonProperty("duration_ms")
        private Integer durationMs;           // ← getDurationMs()
    }
}
