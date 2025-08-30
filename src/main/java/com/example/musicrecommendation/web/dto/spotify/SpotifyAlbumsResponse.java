package com.example.musicrecommendation.web.dto.spotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyAlbumsResponse {
    
    @JsonProperty("items")
    private List<SpotifyAlbum> items;

    public SpotifyAlbumsResponse() {}

    public List<SpotifyAlbum> getItems() {
        return items;
    }

    public void setItems(List<SpotifyAlbum> items) {
        this.items = items;
    }
}