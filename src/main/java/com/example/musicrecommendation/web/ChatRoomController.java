package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @GetMapping("/rooms")
    public ResponseEntity<Object> getRooms() {
        // TODO: 실제 로그인 유저 기반으로 방 목록 조회
        Object userChatRoom = chatRoomService.getUserChatRoom(1L);
        return ResponseEntity.ok(java.util.List.of(userChatRoom));
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<Object> getRoom(@PathVariable String roomId) {
        Object chatHistory = chatRoomService.getChatHistory(roomId, 1L);
        return ResponseEntity.ok(chatHistory);
    }

    // roomId 정규화 헬퍼 메소드 (ChatMessageController와 동일한 로직)
    private Long normalizeRoomId(String roomId) {
        if (roomId == null) return 1L;
        
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