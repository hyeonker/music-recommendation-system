package com.example.musicrecommendation.web.dto.spotify;

public class SpotifyFollowers {
    private String href;
    private Integer total;

    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
}