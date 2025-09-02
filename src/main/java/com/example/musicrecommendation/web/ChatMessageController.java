package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.example.musicrecommendation.web.dto.ChatMessageCreateRequest;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageService.ChatMessageDto>> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "asc") String order
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        boolean asc = !"desc".equalsIgnoreCase(order);
        // roomId를 Long으로 변환하거나 해시 처리
        Long normalizedRoomId = normalizeRoomId(roomId);
        return ResponseEntity.ok(chatMessageService.getMessages(normalizedRoomId, safeLimit, asc));
    }

    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Void> postMessage(
            @PathVariable String roomId,
            @RequestParam(required = true) Long senderId,
            @RequestParam(required = false, defaultValue = "") String content
    ) {
        if (content.trim().isEmpty()) return ResponseEntity.badRequest().build();
        
        // roomId를 Long으로 변환하거나 해시 처리
        Long normalizedRoomId = normalizeRoomId(roomId);
        chatMessageService.saveText(normalizedRoomId, senderId, content.trim());
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/rooms/{roomId}/messages", consumes = "application/json")
    public ResponseEntity<Void> postMessageJson(
            @PathVariable String roomId,
            @RequestBody ChatMessageCreateRequest request
    ) {
        if (request.content().trim().isEmpty()) return ResponseEntity.badRequest().build();
        
        if (request.senderId() == null) {
            throw new IllegalArgumentException("senderId는 필수입니다");
        }
        Long senderId = request.senderId();
        Long normalizedRoomId = normalizeRoomId(roomId);
        chatMessageService.saveText(normalizedRoomId, senderId, request.content().trim());
        return ResponseEntity.ok().build();
    }
    
    // roomId 정규화 헬퍼 메소드
    private Long normalizeRoomId(String roomId) {
        if (roomId == null) throw new IllegalArgumentException("roomId는 필수입니다");
        
        // "room_1_2" -> "12" 또는 해시값으로 변환
        String digits = roomId.replaceAll("\\D", "");
        if (!digits.isEmpty()) {
            try {
                return Long.parseLong(digits.length() > 10 ? digits.substring(0, 10) : digits);
            } catch (NumberFormatException e) {
                // 파싱 실패시 해시값 사용
                return (long) Math.abs(roomId.hashCode());
            }
        }
        
        // 숫자가 없으면 해시값 사용
        return (long) Math.abs(roomId.hashCode());
    }
}
