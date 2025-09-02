package com.example.musicrecommendation.service;

import com.example.musicrecommendation.service.SecureChatRoomService.ChatRoomInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 채팅방 만료 경고 알림 서비스
 * - 30분, 5분, 1분 전 만료 경고 전송
 * - 자동 연장 알림
 * - 중복 알림 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatExpiryNotificationService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final SecureChatRoomService secureChatRoomService;
    
    // 이미 전송된 경고 추적 (roomId_warningType)
    private final Set<String> sentWarnings = ConcurrentHashMap.newKeySet();
    
    /**
     * 만료 경고 알림 전송
     */
    public void sendExpiryWarnings() {
        try {
            // 30분 경고
            sendWarningsForMinutes(30, "30분");
            
            // 5분 경고
            sendWarningsForMinutes(5, "5분");
            
            // 1분 경고
            sendWarningsForMinutes(1, "1분");
            
        } catch (Exception e) {
            log.error("만료 경고 알림 전송 중 오류 발생", e);
        }
    }
    
    /**
     * 특정 시점의 만료 경고 전송
     */
    private void sendWarningsForMinutes(int warningMinutes, String timeText) {
        Map<String, ChatRoomInfo> roomsNeedingWarning = 
            secureChatRoomService.getRoomsNeedingWarning(warningMinutes);
            
        for (Map.Entry<String, ChatRoomInfo> entry : roomsNeedingWarning.entrySet()) {
            String roomId = entry.getKey();
            ChatRoomInfo roomInfo = entry.getValue();
            
            // 중복 경고 방지
            String warningKey = roomId + "_" + warningMinutes;
            if (!sentWarnings.contains(warningKey)) {
                sendExpiryWarningMessage(roomId, roomInfo, timeText);
                sentWarnings.add(warningKey);
                
                log.info("만료 경고 전송 - 방: {}, {}전", roomId, timeText);
            }
        }
    }
    
    /**
     * 만료 경고 메시지 전송
     */
    private void sendExpiryWarningMessage(String roomId, ChatRoomInfo roomInfo, String timeText) {
        long minutesLeft = roomInfo.getMinutesUntilExpiry();
        
        final String finalRoomId = roomId;
        Object warningMessage = new Object() {
            public final String id = "-1";
            public final String roomId = finalRoomId;
            public final int senderId = -1;
            public final String senderName = "System";
            public final String content = String.format(
                "⚠️ 채팅방이 %s 후 자동으로 종료됩니다. (남은 시간: %d분)\n" +
                "메시지를 보내면 1시간 더 연장됩니다.", timeText, minutesLeft);
            public final String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
            public final String type = "EXPIRY_WARNING";
        };
        
        String topic = "/topic/room." + roomId;
        messagingTemplate.convertAndSend(topic, warningMessage);
    }
    
    /**
     * 채팅방 연장 알림 전송
     */
    public void sendRoomExtensionNotification(String roomId) {
        try {
            final String finalRoomId = roomId;
            Object extensionMessage = new Object() {
                public final String id = "-1";
                public final String roomId = finalRoomId;
                public final int senderId = -1;
                public final String senderName = "System";
                public final String content = "✅ 채팅방이 1시간 더 연장되었습니다!";
                public final String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
                public final String type = "ROOM_EXTENDED";
            };
            
            String topic = "/topic/room." + roomId;
            messagingTemplate.convertAndSend(topic, extensionMessage);
            
            // 해당 방의 모든 경고 플래그 리셋
            sentWarnings.removeIf(warningKey -> warningKey.startsWith(roomId + "_"));
            
            log.info("채팅방 연장 알림 전송 - 방: {}", roomId);
            
        } catch (Exception e) {
            log.error("채팅방 연장 알림 전송 실패 - 방: {}", roomId, e);
        }
    }
    
    /**
     * 만료된 채팅방의 경고 플래그 정리
     */
    public void cleanupExpiredWarnings() {
        // 만료된 채팅방들의 경고 플래그 제거
        Map<String, ChatRoomInfo> allRooms = secureChatRoomService.getRoomsNeedingWarning(Integer.MAX_VALUE);
        Set<String> activeRoomIds = allRooms.keySet();
        
        sentWarnings.removeIf(warningKey -> {
            String roomId = warningKey.split("_")[0];
            return !activeRoomIds.contains(roomId);
        });
        
        log.debug("만료된 경고 플래그 정리 완료");
    }
}