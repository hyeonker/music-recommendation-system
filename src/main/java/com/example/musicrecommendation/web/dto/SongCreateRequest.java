package com.example.musicrecommendation.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "신규 곡 등록 요청")
public record SongCreateRequest(
        @Schema(example = "So What") @NotBlank @Size(max = 200) String title,
        @Schema(example = "Miles Davis") @NotBlank @Size(max = 200) String artist
) {}
