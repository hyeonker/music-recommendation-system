package com.example.musicrecommendation.web.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageDto {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String content;      // 복호화된 평문
    private String createdAt;    // ISO 문자열
    private boolean holdForDispute;
}
