package com.example.musicrecommendation.web.dto;

import java.time.Instant;
import java.util.Map;

public record PlaylistShareResponse(
    boolean success,
    String message,
    Map<String, Object> playlistInfo,
    Instant timestamp
) {}