package com.example.musicrecommendation.web.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProfileDto {
    private Long userId;
    private List<Map<String, Object>> favoriteArtists; // [{id,name,image,...}]
    private List<Map<String, Object>> favoriteGenres;  // [{id,name}, ...]
    private List<String> attendedFestivals;            // ["SUMMER SONIC 2024", ...]
    private Map<String, Object> musicPreferences;      // {preferredEra, moodPreferences, ...}
}
