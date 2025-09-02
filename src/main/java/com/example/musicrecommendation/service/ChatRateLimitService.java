package com.example.musicrecommendation.service;

import com.example.musicrecommendation.config.ChatSecurityConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 채팅 시스템 Rate Limiting 서비스
 * DoS 공격 및 스팸 방지를 위한 요청 제한
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRateLimitService {
    
    private final ChatSecurityConfig securityConfig;
    
    // 사용자별 메시지 전송 카운터 (userId -> counter)
    private final Map<Long, MessageRateCounter> userMessageCounters = new ConcurrentHashMap<>();
    
    // IP별 연결 시도 카운터 (ip -> counter) 
    private final Map<String, ConnectionRateCounter> ipConnectionCounters = new ConcurrentHashMap<>();
    
    // 사용자별 WebSocket 연결 카운터
    private final Map<Long, AtomicInteger> userConnectionCounts = new ConcurrentHashMap<>();
    
    /**
     * 메시지 전송 속도 제한 검사
     * @param userId 사용자 ID
     * @return 전송 허용 여부
     */
    public boolean isMessageAllowed(Long userId) {
        if (userId == null) return false;
        
        MessageRateCounter counter = userMessageCounters.computeIfAbsent(userId, 
            k -> new MessageRateCounter());
            
        boolean allowed = counter.isAllowed(
            securityConfig.getMessagesPerMinute(),
            securityConfig.getMessagesPerHour()
        );
        
        if (!allowed) {
            log.warn("메시지 속도 제한 초과 - 사용자: {}", securityConfig.maskUserId(userId));
        }
        
        return allowed;
    }
    
    /**
     * WebSocket 연결 속도 제한 검사
     * @param clientIp 클라이언트 IP
     * @return 연결 허용 여부
     */
    public boolean isConnectionAllowed(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) return false;
        
        ConnectionRateCounter counter = ipConnectionCounters.computeIfAbsent(clientIp,
            k -> new ConnectionRateCounter());
            
        boolean allowed = counter.isAllowed(securityConfig.getConnectionsPerMinute());
        
        if (!allowed) {
            log.warn("연결 속도 제한 초과 - IP: {}", maskIp(clientIp));
        }
        
        return allowed;
    }
    
    /**
     * 사용자별 동시 연결 수 제한 검사
     * @param userId 사용자 ID
     * @return 연결 허용 여부
     */
    public boolean canUserConnect(Long userId) {
        if (userId == null) return false;
        
        AtomicInteger connectionCount = userConnectionCounts.computeIfAbsent(userId,
            k -> new AtomicInteger(0));
            
        int currentConnections = connectionCount.get();
        boolean allowed = currentConnections < securityConfig.getMaxConnectionsPerUser();
        
        if (!allowed) {
            log.warn("동시 연결 수 제한 초과 - 사용자: {} (현재: {}/{})", 
                securityConfig.maskUserId(userId), 
                currentConnections, 
                securityConfig.getMaxConnectionsPerUser());
        }
        
        return allowed;
    }
    
    /**
     * 사용자 연결 추가
     */
    public void addUserConnection(Long userId) {
        if (userId != null) {
            userConnectionCounts.computeIfAbsent(userId, k -> new AtomicInteger(0))
                .incrementAndGet();
        }
    }
    
    /**
     * 사용자 연결 제거
     */
    public void removeUserConnection(Long userId) {
        if (userId != null) {
            AtomicInteger counter = userConnectionCounts.get(userId);
            if (counter != null) {
                int remaining = counter.decrementAndGet();
                if (remaining <= 0) {
                    userConnectionCounts.remove(userId);
                }
            }
        }
    }
    
    /**
     * 메시지 크기 제한 검사
     */
    public boolean isMessageSizeAllowed(String message) {
        if (message == null) return true;
        
        int length = message.length();
        int bytes = message.getBytes().length;
        
        boolean lengthOk = length <= securityConfig.getMaxMessageLength();
        boolean sizeOk = bytes <= securityConfig.getMaxMessageSizeBytes();
        
        if (!lengthOk || !sizeOk) {
            log.warn("메시지 크기 제한 초과 - 길이: {}/{}, 크기: {}/{} bytes", 
                length, securityConfig.getMaxMessageLength(),
                bytes, securityConfig.getMaxMessageSizeBytes());
        }
        
        return lengthOk && sizeOk;
    }
    
    /**
     * 정리 작업 - 오래된 카운터 제거
     */
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        
        // 메시지 카운터 정리
        userMessageCounters.entrySet().removeIf(entry -> 
            entry.getValue().getLastActivity().isBefore(cutoff));
            
        // 연결 카운터 정리  
        ipConnectionCounters.entrySet().removeIf(entry ->
            entry.getValue().getLastActivity().isBefore(cutoff));
            
        log.debug("Rate limit 카운터 정리 완료");
    }
    
    // IP 마스킹 (개인정보 보호)
    private String maskIp(String ip) {
        if (securityConfig.isProductionMode() && ip != null && ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + ".***.***.***";
            }
        }
        return ip;
    }
    
    /**
     * 메시지 전송 속도 카운터
     */
    private static class MessageRateCounter {
        private volatile int minuteCount = 0;
        private volatile int hourCount = 0;
        private volatile LocalDateTime lastMinute = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        private volatile LocalDateTime lastHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        private volatile LocalDateTime lastActivity = LocalDateTime.now();
        
        public synchronized boolean isAllowed(int maxPerMinute, int maxPerHour) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime currentMinute = now.truncatedTo(ChronoUnit.MINUTES);
            LocalDateTime currentHour = now.truncatedTo(ChronoUnit.HOURS);
            
            // 분 카운터 리셋
            if (!currentMinute.equals(lastMinute)) {
                minuteCount = 0;
                lastMinute = currentMinute;
            }
            
            // 시간 카운터 리셋
            if (!currentHour.equals(lastHour)) {
                hourCount = 0;
                lastHour = currentHour;
            }
            
            // 제한 확인
            if (minuteCount >= maxPerMinute || hourCount >= maxPerHour) {
                return false;
            }
            
            // 카운터 증가
            minuteCount++;
            hourCount++;
            lastActivity = now;
            
            return true;
        }
        
        public LocalDateTime getLastActivity() {
            return lastActivity;
        }
    }
    
    /**
     * 연결 시도 속도 카운터
     */
    private static class ConnectionRateCounter {
        private volatile int minuteCount = 0;
        private volatile LocalDateTime lastMinute = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        private volatile LocalDateTime lastActivity = LocalDateTime.now();
        
        public synchronized boolean isAllowed(int maxPerMinute) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime currentMinute = now.truncatedTo(ChronoUnit.MINUTES);
            
            // 분 카운터 리셋
            if (!currentMinute.equals(lastMinute)) {
                minuteCount = 0;
                lastMinute = currentMinute;
            }
            
            // 제한 확인
            if (minuteCount >= maxPerMinute) {
                return false;
            }
            
            // 카운터 증가
            minuteCount++;
            lastActivity = now;
            
            return true;
        }
        
        public LocalDateTime getLastActivity() {
            return lastActivity;
        }
    }
}