package com.example.musicrecommendation.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * 보안이 강화된 채팅방 관리 서비스
 * - UUID 기반 안전한 roomId 생성
 * - 사용자 권한 검증
 * - 세션 만료 관리
 */
@Service
@RequiredArgsConstructor
public class SecureChatRoomService {
    
    private static final Logger log = LoggerFactory.getLogger(SecureChatRoomService.class);
    
    // 채팅방 정보 저장
    private final Map<String, ChatRoomInfo> chatRooms = new ConcurrentHashMap<>();
    
    // 사용자별 활성 채팅방 매핑 (userId -> roomId)
    private final Map<Long, String> userToRoom = new ConcurrentHashMap<>();
    
    // 채팅방별 참여자 매핑 (roomId -> Set<userId>)  
    private final Map<String, Set<Long>> roomToUsers = new ConcurrentHashMap<>();
    
    // 보안 강화를 위한 SecureRandom
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * 안전한 채팅방 ID 생성
     * UUID 기반으로 예측 불가능한 ID 생성
     */
    public String generateSecureRoomId() {
        // UUID v4 사용 (랜덤 기반)
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * 채팅방 생성 (매칭 성공 시)
     * @param user1Id 첫 번째 사용자 ID
     * @param user2Id 두 번째 사용자 ID
     * @return 생성된 채팅방 정보
     */
    public ChatRoomCreationResult createSecureChatRoom(Long user1Id, Long user2Id) {
        // 기존 활성 채팅방 확인
        String existingRoom = findExistingRoom(user1Id, user2Id);
        if (existingRoom != null) {
            ChatRoomInfo existing = chatRooms.get(existingRoom);
            if (existing != null && existing.isActive() && !existing.isExpired()) {
                log.info("기존 채팅방 재사용: {} (users: {}, {})", existingRoom, user1Id, user2Id);
                return new ChatRoomCreationResult(existingRoom, existing, false);
            }
        }
        
        // 새 채팅방 생성
        String secureRoomId = generateSecureRoomId();
        
        ChatRoomInfo roomInfo = ChatRoomInfo.builder()
            .roomId(secureRoomId)
            .participant1(user1Id)
            .participant2(user2Id)
            .createdAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
            .lastActivityAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
            .active(true)
            .roomType("PRIVATE_MATCH")
            .build();
        
        // 매핑 정보 저장
        chatRooms.put(secureRoomId, roomInfo);
        userToRoom.put(user1Id, secureRoomId);
        userToRoom.put(user2Id, secureRoomId);
        roomToUsers.put(secureRoomId, Set.of(user1Id, user2Id));
        
        log.info("새 보안 채팅방 생성: {} (users: {}, {})", secureRoomId, user1Id, user2Id);
        
        return new ChatRoomCreationResult(secureRoomId, roomInfo, true);
    }
    
    /**
     * 채팅방 접근 권한 확인
     * @param roomId 채팅방 ID (UUID 기반만 지원)
     * @param userId 사용자 ID  
     * @return 접근 권한 여부
     */
    public boolean hasRoomAccess(String roomId, Long userId) {
        if (roomId == null || userId == null) {
            return false;
        }
        
        ChatRoomInfo room = chatRooms.get(roomId);
        if (room == null || !room.isActive() || room.isExpired()) {
            log.warn("채팅방 접근 거부 - 존재하지 않거나 비활성/만료됨: {}", roomId);
            return false;
        }
        
        boolean hasAccess = room.getParticipant1().equals(userId) || 
                           room.getParticipant2().equals(userId);
        
        if (!hasAccess) {
            log.warn("채팅방 접근 거부 - 권한 없음: {} (user: {})", roomId, userId);
        }
        
        return hasAccess;
    }
    
    /**
     * 채팅방 활동 업데이트 (메시지 전송 시)
     */
    public void updateRoomActivity(String roomId) {
        ChatRoomInfo room = chatRooms.get(roomId);
        if (room != null) {
            room.updateLastActivity();
        }
    }
    
    /**
     * 사용자의 활성 채팅방 조회
     */
    public String getUserActiveRoom(Long userId) {
        String roomId = userToRoom.get(userId);
        if (roomId != null) {
            ChatRoomInfo room = chatRooms.get(roomId);
            if (room != null && room.isActive() && !room.isExpired()) {
                return roomId;
            }
            // 만료된 방 정리
            userToRoom.remove(userId);
        }
        return null;
    }
    
    /**
     * 기존 채팅방 찾기 (같은 사용자 조합)
     */
    private String findExistingRoom(Long user1Id, Long user2Id) {
        String room1 = userToRoom.get(user1Id);
        String room2 = userToRoom.get(user2Id);
        
        if (room1 != null && room1.equals(room2)) {
            return room1;
        }
        return null;
    }
    
    /**
     * 만료된 채팅방 정리 (스케줄링으로 호출)
     */
    public int cleanupExpiredRooms() {
        int cleaned = 0;
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        
        for (Map.Entry<String, ChatRoomInfo> entry : chatRooms.entrySet()) {
            ChatRoomInfo room = entry.getValue();
            if (room.isExpired()) {
                String roomId = entry.getKey();
                
                // 매핑 정보 정리
                userToRoom.remove(room.getParticipant1());
                userToRoom.remove(room.getParticipant2());
                roomToUsers.remove(roomId);
                chatRooms.remove(roomId);
                
                cleaned++;
                log.info("만료된 채팅방 정리: {} (created: {}, lastActivity: {})", 
                    roomId, room.getCreatedAt(), room.getLastActivityAt());
            }
        }
        
        if (cleaned > 0) {
            log.info("채팅방 정리 완료: {}개 방 삭제", cleaned);
        }
        
        return cleaned;
    }
    
    /**
     * 만료 경고가 필요한 채팅방들 조회
     * @param warningMinutes 경고 시점 (분)
     * @return 경고 필요한 채팅방 목록
     */
    public Map<String, ChatRoomInfo> getRoomsNeedingWarning(int warningMinutes) {
        Map<String, ChatRoomInfo> needingWarning = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, ChatRoomInfo> entry : chatRooms.entrySet()) {
            ChatRoomInfo room = entry.getValue();
            if (room.needsWarning(warningMinutes)) {
                needingWarning.put(entry.getKey(), room);
            }
        }
        
        return needingWarning;
    }
    
    /**
     * 채팅방 활동 업데이트 및 자동 연장 알림
     */
    public boolean updateRoomActivityWithExtensionCheck(String roomId) {
        ChatRoomInfo room = chatRooms.get(roomId);
        if (room != null) {
            // 만료 임박 상태였는지 확인 (5분 이내)
            boolean wasExpiringSoon = room.needsWarning(5);
            
            room.updateLastActivity();
            
            // 만료 임박 상태에서 메시지가 와서 연장된 경우 true 반환
            return wasExpiringSoon;
        }
        return false;
    }
    
    /**
     * 사용자가 채팅방에서 나가기
     * @param roomId 채팅방 ID
     * @param userId 나가는 사용자 ID
     * @return 성공 여부
     */
    public boolean leaveRoom(String roomId, Long userId) {
        if (roomId == null || userId == null) {
            return false;
        }
        
        ChatRoomInfo room = chatRooms.get(roomId);
        if (room == null) {
            log.warn("존재하지 않는 채팅방 나가기 시도: {}", roomId);
            return false;
        }
        
        // 해당 사용자가 참여자인지 확인
        if (!room.getParticipant1().equals(userId) && !room.getParticipant2().equals(userId)) {
            log.warn("채팅방 참여자가 아닌 사용자의 나가기 시도: {} (user: {})", roomId, userId);
            return false;
        }
        
        // 채팅방을 비활성화하고 매핑 정보 제거
        room.deactivate();
        userToRoom.remove(userId);
        
        // 상대방도 매핑에서 제거 (채팅방 자체를 종료)
        Long otherUserId = room.getParticipant1().equals(userId) ? room.getParticipant2() : room.getParticipant1();
        userToRoom.remove(otherUserId);
        roomToUsers.remove(roomId);
        
        log.info("사용자 채팅방 나가기 완료: {} (user: {}, room deactivated)", roomId, userId);
        return true;
    }
    
    /**
     * 채팅방 정보 조회
     * @param roomId 채팅방 ID
     * @return 채팅방 정보
     */
    public ChatRoomInfo getRoomInfo(String roomId) {
        return chatRooms.get(roomId);
    }
    
    /**
     * 채팅방 정보 클래스
     */
    public static class ChatRoomInfo {
        private final String roomId;
        private final Long participant1;
        private final Long participant2;
        private final LocalDateTime createdAt;
        private volatile LocalDateTime lastActivityAt;
        private volatile boolean active;
        private final String roomType;
        
        // 채팅방 만료 시간 (1시간)
        private static final long ROOM_EXPIRY_HOURS = 1;
        
        // KST 타임존
        private static final ZoneId KST = ZoneId.of("Asia/Seoul");
        
        private ChatRoomInfo(String roomId, Long participant1, Long participant2, 
                           LocalDateTime createdAt, LocalDateTime lastActivityAt,
                           boolean active, String roomType) {
            this.roomId = roomId;
            this.participant1 = participant1;
            this.participant2 = participant2;
            this.createdAt = createdAt;
            this.lastActivityAt = lastActivityAt;
            this.active = active;
            this.roomType = roomType;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public void updateLastActivity() {
            this.lastActivityAt = LocalDateTime.now(KST);
        }
        
        public void deactivate() {
            this.active = false;
        }
        
        public boolean isExpired() {
            return ChronoUnit.HOURS.between(lastActivityAt, LocalDateTime.now(KST)) > ROOM_EXPIRY_HOURS;
        }
        
        /**
         * 만료까지 남은 시간 (분 단위)
         */
        public long getMinutesUntilExpiry() {
            long minutesSinceActivity = ChronoUnit.MINUTES.between(lastActivityAt, LocalDateTime.now(KST));
            long expiryMinutes = ROOM_EXPIRY_HOURS * 60;
            return Math.max(0, expiryMinutes - minutesSinceActivity);
        }
        
        /**
         * 만료 경고가 필요한지 확인
         * @param warningMinutes 경고 시점 (분)
         * @return 경고 필요 여부
         */
        public boolean needsWarning(int warningMinutes) {
            long minutesLeft = getMinutesUntilExpiry();
            return minutesLeft <= warningMinutes && minutesLeft > 0 && isActive();
        }
        
        // Getters
        public String getRoomId() { return roomId; }
        public Long getParticipant1() { return participant1; }
        public Long getParticipant2() { return participant2; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastActivityAt() { return lastActivityAt; }
        public boolean isActive() { return active; }
        public String getRoomType() { return roomType; }
        
        public static class Builder {
            private String roomId;
            private Long participant1;
            private Long participant2;
            private LocalDateTime createdAt;
            private LocalDateTime lastActivityAt;
            private boolean active;
            private String roomType;
            
            public Builder roomId(String roomId) { this.roomId = roomId; return this; }
            public Builder participant1(Long participant1) { this.participant1 = participant1; return this; }
            public Builder participant2(Long participant2) { this.participant2 = participant2; return this; }
            public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
            public Builder lastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; return this; }
            public Builder active(boolean active) { this.active = active; return this; }
            public Builder roomType(String roomType) { this.roomType = roomType; return this; }
            
            public ChatRoomInfo build() {
                return new ChatRoomInfo(roomId, participant1, participant2, 
                                      createdAt, lastActivityAt, active, roomType);
            }
        }
    }
    
    /**
     * 채팅방 생성 결과
     */
    public static class ChatRoomCreationResult {
        private final String roomId;
        private final ChatRoomInfo roomInfo;
        private final boolean newRoom;
        
        public ChatRoomCreationResult(String roomId, ChatRoomInfo roomInfo, boolean newRoom) {
            this.roomId = roomId;
            this.roomInfo = roomInfo;
            this.newRoom = newRoom;
        }
        
        public String getRoomId() { return roomId; }
        public ChatRoomInfo getRoomInfo() { return roomInfo; }
        public boolean isNewRoom() { return newRoom; }
    }
}