package com.example.musicrecommendation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

/**
 * 채팅 시스템 보안 설정
 * 운영 환경에서 중요한 보안 파라미터들을 중앙 관리
 */
@Configuration  
@Getter
public class ChatSecurityConfig {
    
    // ===== Rate Limiting 설정 =====
    
    /** 사용자당 분당 최대 메시지 전송 수 */
    @Value("${app.chat.rate-limit.messages-per-minute:15}")
    private int messagesPerMinute;
    
    /** 사용자당 시간당 최대 메시지 전송 수 */
    @Value("${app.chat.rate-limit.messages-per-hour:300}")
    private int messagesPerHour;
    
    /** IP당 분당 최대 WebSocket 연결 시도 수 */
    @Value("${app.chat.rate-limit.connections-per-minute:10}")
    private int connectionsPerMinute;
    
    // ===== 메시지 제한 설정 =====
    
    /** 메시지 최대 길이 (문자 수) */
    @Value("${app.chat.limits.max-message-length:300}")
    private int maxMessageLength;
    
    /** 메시지 최대 크기 (바이트) */
    @Value("${app.chat.limits.max-message-size:1000}")
    private int maxMessageSizeBytes;
    
    /** 사용자당 동시 WebSocket 연결 최대 수 */
    @Value("${app.chat.limits.max-connections-per-user:3}")
    private int maxConnectionsPerUser;
    
    // ===== 세션 보안 설정 =====
    
    /** WebSocket 세션 최대 유지 시간 (분) */
    @Value("${app.chat.session.max-idle-minutes:30}")
    private int maxIdleMinutes;
    
    /** 채팅방 비활성 상태에서 자동 종료 시간 (분) */
    @Value("${app.chat.session.auto-close-minutes:60}")
    private int autoCloseMinutes;
    
    // ===== 로깅 보안 설정 =====
    
    /** 민감정보 로깅 여부 (개발환경에서만 true) */
    @Value("${app.chat.logging.enable-sensitive-data:false}")
    private boolean enableSensitiveDataLogging;
    
    /** 상세 디버그 로깅 여부 */
    @Value("${app.chat.logging.enable-debug:false}")
    private boolean enableDebugLogging;
    
    // ===== IP 보안 설정 =====
    
    /** 신뢰할 수 있는 프록시 IP 목록 (로드밸런서 등) */
    @Value("${app.chat.security.trusted-proxies:127.0.0.1,10.0.0.0/8}")
    private String trustedProxies;
    
    /** 차단할 IP 목록 */
    @Value("${app.chat.security.blocked-ips:}")
    private String blockedIps;
    
    /**
     * 개발/운영 환경에 따른 보안 수준 조정
     */
    public boolean isProductionMode() {
        return !enableSensitiveDataLogging && !enableDebugLogging;
    }
    
    /**
     * 안전한 로깅을 위한 사용자 ID 마스킹
     */
    public String maskUserId(Long userId) {
        if (enableSensitiveDataLogging || userId == null) {
            return String.valueOf(userId);
        }
        return "user***" + (userId % 1000); // 마지막 3자리만 노출
    }
    
    /**
     * 안전한 로깅을 위한 룸 ID 마스킹  
     */
    public String maskRoomId(String roomId) {
        if (enableSensitiveDataLogging || roomId == null || roomId.length() < 8) {
            return roomId;
        }
        return roomId.substring(0, 4) + "****" + roomId.substring(roomId.length() - 4);
    }
}