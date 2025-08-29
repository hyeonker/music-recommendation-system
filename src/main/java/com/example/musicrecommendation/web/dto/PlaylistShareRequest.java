package com.example.musicrecommendation.web.dto;

import jakarta.validation.constraints.NotNull;

public record PlaylistShareRequest(
    @NotNull Long fromUserId,
    @NotNull Long toUserId,
    @NotNull String playlistId,
    String playlistName,
    String description,
    String message
) {}