// src/main/java/com/example/musicrecommendation/web/dto/ChatMessageResponse.java
package com.example.musicrecommendation.web.dto;

import java.time.OffsetDateTime;

public record ChatMessageResponse(
        Long id,
        Long roomId,
        Long senderId,
        String content,              // 복호화된 평문
        OffsetDateTime createdAt,
        String type                  // "TEXT" 고정
) {}
