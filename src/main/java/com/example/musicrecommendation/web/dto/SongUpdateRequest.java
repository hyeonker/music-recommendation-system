package com.example.musicrecommendation.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "곡 수정 요청")
public record SongUpdateRequest(
        @Schema(example = "So What (Remastered)") @NotBlank @Size(max = 200) String title,
        @Schema(example = "Miles Davis") @NotBlank @Size(max = 200) String artist,
        @Schema(example = "0", description = "낙관적 락 버전") @NotNull Long version
) {}
