package com.example.musicrecommendation.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SongCreateRequest(
        @NotBlank(message = "title은 필수입니다.") String title,
        @NotBlank(message = "artist는 필수입니다.") String artist
) {}
