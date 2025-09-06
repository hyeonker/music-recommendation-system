package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.ChatMessage;
import com.example.musicrecommendation.repository.ChatMessageRepository;
import com.example.musicrecommendation.security.AESGcmTextEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository repository;
    private final AESGcmTextEncryptor encryptor;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessages(String roomId, int limit, boolean asc) {
        var sort = Sort.by(asc ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt");
        var pageable = PageRequest.of(0, Math.max(1, Math.min(limit, 100)), sort);

        var list = repository.findByRoomId(roomId, pageable).stream()
                .map(m -> new ChatMessageDto(
                        m.getId(),
                        m.getRoomId(),
                        m.getSenderId(),
                        decryptSafely(m.getContentCipher(), m.getIvBase64()), // 암호문 + IV
                        m.getCreatedAt() != null ? m.getCreatedAt().toInstant() : Instant.now()
                ))
                .toList();

        return list.stream().sorted(asc
                ? Comparator.comparing(ChatMessageDto::createdAt)
                : Comparator.comparing(ChatMessageDto::createdAt).reversed()).toList();
    }

    @Transactional
    public ChatMessageDto saveText(String roomId, Long senderId, String plaintext) {
        var m = new ChatMessage();
        m.setRoomId(roomId);
        m.setSenderId(senderId);

        String[] enc = encryptor.encrypt(plaintext); // [cipher, iv]
        m.setContentCipher(enc[0]);
        m.setIvBase64(enc[1]);

        if (m.getCreatedAt() == null) {
            m.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }

        var saved = repository.save(m);
        var result = new ChatMessageDto(
                saved.getId(),
                saved.getRoomId(),
                saved.getSenderId(),
                decryptSafely(saved.getContentCipher(), saved.getIvBase64()),
                saved.getCreatedAt() != null ? saved.getCreatedAt().toInstant() : Instant.now()
        );
        
        // 채팅 메시지 알림 전송 (비동기)
        try {
            // roomId에서 수신자 추정 (실제 구현에서는 채팅방 정보를 조회해야 함)
            Long receiverId = extractReceiverIdFromRoom(roomId, senderId);
            if (receiverId != null) {
                String senderName = "사용자#" + senderId;
                notificationService.sendMessageNotification(receiverId, senderId, senderName, plaintext);
            }
        } catch (Exception e) {
            // 알림 전송 실패해도 메시지 저장은 성공으로 처리
        }
        
        return result;
    }

    @Transactional
    public int purgeOlderThanDays(int days) {
        var threshold = OffsetDateTime.now(ZoneOffset.UTC).minus(days, ChronoUnit.DAYS);
        return repository.deleteOlderThan(threshold);
    }
    
    @Transactional
    public int purgeOlderThan90Days() {
        return purgeOlderThanDays(90);
    }

    @Transactional
    public int purgeOlderThanMonths(int months) {
        var threshold = OffsetDateTime.now(ZoneOffset.UTC).minus(months * 30L, ChronoUnit.DAYS);
        return repository.deleteOlderThan(threshold);
    }

    private String decryptSafely(String cipherB64, String ivB64) {
        try {
            return encryptor.decrypt(cipherB64, ivB64);
        } catch (Exception e) {
            return "[decrypt_error]";
        }
    }
    
    private Long extractReceiverIdFromRoom(String roomId, Long senderId) {
        // 간단한 roomId 파싱 로직 (예: roomId가 두 사용자 ID의 조합인 경우)
        // 실제 구현에서는 채팅방 정보 테이블을 조회해야 함
        try {
            // hex string roomId에서 사용자 ID 추출하는 로직
            // 실제 구현에서는 채팅방 정보 테이블을 조회해야 함
            if (roomId != null && roomId.length() >= 2) {
                // 단순 예제: roomId의 마지막 문자를 숫자로 변환 시도
                String lastChar = roomId.substring(roomId.length() - 1);
                if (lastChar.matches("[0-9]")) {
                    Long possibleReceiverId = Long.parseLong(lastChar);
                    return possibleReceiverId.equals(senderId) ? null : possibleReceiverId;
                }
            }
        } catch (Exception e) {
            // 파싱 실패시 null 반환
        }
        return null;
    }

    /**
     * 해당 채팅방에서 senderId가 아닌 다른 참여자 찾기
     */
    @Transactional(readOnly = true)
    public Long findOtherParticipant(String roomId, Long senderId) {
        try {
            List<Long> participants = repository.findDistinctSenderIdByRoomId(roomId);
            System.out.println("[ChatMessageService] 채팅방 " + roomId + " 참여자들: " + participants);
            
            for (Long participantId : participants) {
                if (!participantId.equals(senderId)) {
                    System.out.println("[ChatMessageService] 찾은 다른 참여자: " + participantId);
                    return participantId;
                }
            }
            
            System.out.println("[ChatMessageService] 다른 참여자를 찾지 못함");
            return null;
            
        } catch (Exception e) {
            System.err.println("[ChatMessageService] findOtherParticipant 실패: " + e.getMessage());
            return null;
        }
    }

    public record ChatMessageDto(Long id, String roomId, Long senderId, String content, Instant createdAt) { }
}
