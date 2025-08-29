package com.example.musicrecommendation.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageCreateRequest(
        @NotBlank String content,
        Long senderId // 선택: 없으면 컨트롤러에서 기본값 처리
) {}
