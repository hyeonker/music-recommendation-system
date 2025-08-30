package com.example.musicrecommendation.web.dto;

import com.example.musicrecommendation.domain.Song;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "곡 응답")
public record SongResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "So What") String title,
        @Schema(example = "Miles Davis") String artist,
        @Schema(example = "https://i.scdn.co/image/ab67616d0000b273...") String imageUrl,
        @Schema(example = "0") Long version
) {
    public static SongResponse from(Song s) {
        return new SongResponse(s.getId(), s.getTitle(), s.getArtist(), s.getImageUrl(), s.getVersion());
    }
}
