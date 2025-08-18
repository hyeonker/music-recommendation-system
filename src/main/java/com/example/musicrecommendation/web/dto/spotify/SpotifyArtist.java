package com.example.musicrecommendation.web.dto.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SpotifyArtist {
    private String id;
    private String name;
    private List<SpotifyImage> images;
    private SpotifyFollowers followers;
    private List<String> genres;
    private String href;
    private Integer popularity;
    private String type;
    private String uri;

    @JsonProperty("external_urls")
    private SpotifyExternalUrls externalUrls;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<SpotifyImage> getImages() { return images; }
    public void setImages(List<SpotifyImage> images) { this.images = images; }

    public SpotifyFollowers getFollowers() { return followers; }
    public void setFollowers(SpotifyFollowers followers) { this.followers = followers; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }

    public Integer getPopularity() { return popularity; }
    public void setPopularity(Integer popularity) { this.popularity = popularity; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public SpotifyExternalUrls getExternalUrls() { return externalUrls; }
    public void setExternalUrls(SpotifyExternalUrls externalUrls) { this.externalUrls = externalUrls; }
}