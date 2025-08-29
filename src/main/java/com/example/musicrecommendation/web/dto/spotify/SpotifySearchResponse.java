package com.example.musicrecommendation.web.dto.spotify;

import java.util.List;

public class SpotifySearchResponse {
    private Artists artists;
    private Tracks tracks;

    public Artists getArtists() { return artists; }
    public void setArtists(Artists artists) { this.artists = artists; }

    public Tracks getTracks() { return tracks; }
    public void setTracks(Tracks tracks) { this.tracks = tracks; }

    public static class Artists {
        private String href;
        private List<SpotifyArtist> items;
        private Integer limit;
        private String next;
        private Integer offset;
        private String previous;
        private Integer total;

        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }

        public List<SpotifyArtist> getItems() { return items; }
        public void setItems(List<SpotifyArtist> items) { this.items = items; }

        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }

        public String getNext() { return next; }
        public void setNext(String next) { this.next = next; }

        public Integer getOffset() { return offset; }
        public void setOffset(Integer offset) { this.offset = offset; }

        public String getPrevious() { return previous; }
        public void setPrevious(String previous) { this.previous = previous; }

        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }
    }

    public static class Tracks {
        private String href;
        private List<SpotifyTrack> items;
        private Integer limit;
        private String next;
        private Integer offset;
        private String previous;
        private Integer total;

        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }

        public List<SpotifyTrack> getItems() { return items; }
        public void setItems(List<SpotifyTrack> items) { this.items = items; }

        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }

        public String getNext() { return next; }
        public void setNext(String next) { this.next = next; }

        public Integer getOffset() { return offset; }
        public void setOffset(Integer offset) { this.offset = offset; }

        public String getPrevious() { return previous; }
        public void setPrevious(String previous) { this.previous = previous; }

        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }
    }
}