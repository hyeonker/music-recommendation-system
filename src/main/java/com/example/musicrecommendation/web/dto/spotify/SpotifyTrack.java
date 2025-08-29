package com.example.musicrecommendation.web.dto.spotify;

import java.util.List;

public class SpotifyTrack {
    private String id;
    private String name;
    private String href;
    private Integer popularity;
    private Integer durationMs;
    private Boolean explicit;
    private String previewUrl;
    private Integer trackNumber;
    private String type;
    private String uri;
    private Boolean isLocal;
    private List<SpotifyArtist> artists;
    private SpotifyAlbum album;
    private SpotifyExternalUrls externalUrls;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }

    public Integer getPopularity() { return popularity; }
    public void setPopularity(Integer popularity) { this.popularity = popularity; }

    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }

    public Boolean getExplicit() { return explicit; }
    public void setExplicit(Boolean explicit) { this.explicit = explicit; }

    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }

    public Integer getTrackNumber() { return trackNumber; }
    public void setTrackNumber(Integer trackNumber) { this.trackNumber = trackNumber; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public Boolean getIsLocal() { return isLocal; }
    public void setIsLocal(Boolean isLocal) { this.isLocal = isLocal; }

    public List<SpotifyArtist> getArtists() { return artists; }
    public void setArtists(List<SpotifyArtist> artists) { this.artists = artists; }

    public SpotifyAlbum getAlbum() { return album; }
    public void setAlbum(SpotifyAlbum album) { this.album = album; }

    public SpotifyExternalUrls getExternalUrls() { return externalUrls; }
    public void setExternalUrls(SpotifyExternalUrls externalUrls) { this.externalUrls = externalUrls; }
}