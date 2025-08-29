package com.example.musicrecommendation.web.dto.spotify;

import java.util.List;

public class SpotifyAlbum {
    private String id;
    private String name;
    private String albumType;
    private Integer totalTracks;
    private List<String> availableMarkets;
    private String href;
    private List<SpotifyImage> images;
    private String releaseDate;
    private String releaseDatePrecision;
    private String type;
    private String uri;
    private List<SpotifyArtist> artists;
    private SpotifyExternalUrls externalUrls;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAlbumType() { return albumType; }
    public void setAlbumType(String albumType) { this.albumType = albumType; }

    public Integer getTotalTracks() { return totalTracks; }
    public void setTotalTracks(Integer totalTracks) { this.totalTracks = totalTracks; }

    public List<String> getAvailableMarkets() { return availableMarkets; }
    public void setAvailableMarkets(List<String> availableMarkets) { this.availableMarkets = availableMarkets; }

    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }

    public List<SpotifyImage> getImages() { return images; }
    public void setImages(List<SpotifyImage> images) { this.images = images; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

    public String getReleaseDatePrecision() { return releaseDatePrecision; }
    public void setReleaseDatePrecision(String releaseDatePrecision) { this.releaseDatePrecision = releaseDatePrecision; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public List<SpotifyArtist> getArtists() { return artists; }
    public void setArtists(List<SpotifyArtist> artists) { this.artists = artists; }

    public SpotifyExternalUrls getExternalUrls() { return externalUrls; }
    public void setExternalUrls(SpotifyExternalUrls externalUrls) { this.externalUrls = externalUrls; }
}