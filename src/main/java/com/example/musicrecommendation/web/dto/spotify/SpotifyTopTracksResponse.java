package com.example.musicrecommendation.web.dto.spotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyTopTracksResponse {
    
    @JsonProperty("tracks")
    private List<SpotifyTrack> tracks;

    public SpotifyTopTracksResponse() {}

    public List<SpotifyTrack> getTracks() {
        return tracks;
    }

    public void setTracks(List<SpotifyTrack> tracks) {
        this.tracks = tracks;
    }
}