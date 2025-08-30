package com.example.musicrecommendation.web.dto.spotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyAlbumTracksResponse {
    
    @JsonProperty("items")
    private List<SpotifyTrack> items;

    public SpotifyAlbumTracksResponse() {}

    public List<SpotifyTrack> getItems() {
        return items;
    }

    public void setItems(List<SpotifyTrack> items) {
        this.items = items;
    }
}