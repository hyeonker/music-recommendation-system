package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.MatchingQueueService;
import com.example.musicrecommendation.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 실시간 매칭 REST API 컨트롤러
 */
@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}, allowCredentials = "true")
@RequestMapping("/api/realtime-matching")
@Tag(name = "🔥 Realtime Matching", description = "실시간 1:1 음악 매칭 시스템")
public class RealtimeMatchingController {

    private final MatchingQueueService matchingQueueService;
    private final ChatRoomService chatRoomService;

    public RealtimeMatchingController(MatchingQueueService matchingQueueService,
                                      ChatRoomService chatRoomService) {
        this.matchingQueueService = matchingQueueService;
        this.chatRoomService = chatRoomService;
    }

    @PostMapping("/request/{userId}")
    @Operation(summary = "🔍 매칭 요청", description = "음악 취향 기반 1:1 매칭 요청")
    public ResponseEntity<?> requestMatching(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {
        try {
            Object result = matchingQueueService.requestMatching(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "매칭 요청 처리 중 오류 발생";
                public final String error = e.getMessage();
            });
        }
    }

    @DeleteMapping("/cancel/{userId}")
    @Operation(summary = "❌ 매칭 취소", description = "매칭 대기를 취소합니다")
    public ResponseEntity<?> cancelMatching(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {
        try {
            Object result = matchingQueueService.cancelMatching(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "매칭 취소 처리 중 오류 발생";
                public final String error = e.getMessage();
            });
        }
    }

    @DeleteMapping("/end/{userId}")
    @Operation(summary = "🏁 매칭 종료", description = "현재 매칭을 종료하고 채팅방을 나갑니다")
    public ResponseEntity<?> endMatching(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {
        try {
            Object result = matchingQueueService.endMatch(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "매칭 종료 처리 중 오류 발생";
                public final String error = e.getMessage();
            });
        }
    }

    @GetMapping("/status/{userId}")
    @Operation(summary = "📊 매칭 상태 조회", description = "사용자의 현재 매칭 상태를 조회합니다")
    public ResponseEntity<?> getMatchingStatus(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {
        try {
            Object result = matchingQueueService.getMatchingStatus(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "상태 조회 중 오류 발생";
                public final String error = e.getMessage();
            });
        }
    }

    @GetMapping("/system-status")
    @Operation(summary = "🔧 시스템 상태", description = "전체 매칭 시스템 상태를 조회합니다")
    public ResponseEntity<?> getSystemStatus() {
        try {
            Object matchingStatus = matchingQueueService.getSystemStatus();
            Object chatStatus = chatRoomService.getChatRoomStats();

            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "🔥 실시간 매칭 시스템 가동 중";
                public final Object matchingSystem = matchingStatus;
                public final Object chatSystem = chatStatus;
                public final String systemVersion = "v1.0.0";
            });
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "시스템 상태 조회 중 오류 발생";
                public final String error = e.getMessage();
            });
        }
    }

    // ===== 채팅 관련 API =====

    @GetMapping("/chat/room/{userId}")
    @Operation(summary = "💬 채팅방 정보", description = "사용자의 현재 채팅방 정보를 조회합니다")
    public ResponseEntity<?> getChatRoomInfo(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {
        try {
            Object result = chatRoomService.getUserChatRoom(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "채팅방 정보 조회 중 오류 발생";
                public final String error = e.getMessage();
            });
        }
    }

    @GetMapping("/chat/history/{roomId}")
    @Operation(summary = "📜 채팅 히스토리", description = "채팅방의 메시지 히스토리를 조회합니다")
    public ResponseEntity<?> getChatHistory(
            @Parameter(description = "채팅방 ID") @PathVariable String roomId,
            @Parameter(description = "사용자 ID") @RequestParam Long userId) {
        try {
            Object result = chatRoomService.getChatHistory(roomId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "채팅 히스토리 조회 중 오류 발생";
                public final String error = e.getMessage();
            });
        }
    }

    @PostMapping("/chat/send")
    @Operation(summary = "💌 메시지 전송", description = "채팅방에 메시지를 전송합니다")
    public ResponseEntity<?> sendMessage(
            @Parameter(description = "채팅 메시지 정보") @RequestBody Object messageRequest) {
        try {
            // 실제로는 MessageRequest DTO를 사용하지만, 지금은 간단하게 처리
            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "메시지 전송은 WebSocket을 통해 이루어집니다";
                public final String websocketEndpoint = "/app/send-chat-message";
                public final String instruction = "WebSocket 연결 후 /app/send-chat-message로 메시지를 전송하세요";
            });
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "메시지 전송 처리 중 오류 발생";
                public final String error = e.getMessage();
            });
        }
    }

    @PostMapping("/chat/share-music")
    @Operation(summary = "🎵 음악 공유", description = "채팅방에 음악을 공유합니다")
    public ResponseEntity<?> shareMusicInChat(
            @Parameter(description = "음악 공유 정보") @RequestBody Object musicShareRequest) {
        try {
            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "음악 공유는 WebSocket을 통해 이루어집니다";
                public final String websocketEndpoint = "/app/share-music";
                public final String instruction = "WebSocket 연결 후 /app/share-music으로 음악 정보를 전송하세요";
            });
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "음악 공유 처리 중 오류 발생";
                public final String error = e.getMessage();
            });
        }
    }

    @DeleteMapping("/chat/leave/{roomId}")
    @Operation(summary = "🚪 채팅방 나가기", description = "채팅방을 나갑니다")
    public ResponseEntity<?> leaveChatRoom(
            @Parameter(description = "채팅방 ID") @PathVariable String roomId,
            @Parameter(description = "사용자 ID") @RequestParam Long userId) {
        try {
            Object result = chatRoomService.leaveChatRoom(roomId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "채팅방 나가기 처리 중 오류 발생";
                public final String error = e.getMessage();
            });
        }
    }

    // ===== 테스트 및 데모 API =====

    @PostMapping("/demo/quick-match")
    @Operation(summary = "⚡ 빠른 매칭 데모", description = "테스트용 빠른 매칭 (개발용)")
    public ResponseEntity<?> quickMatchDemo(
            @Parameter(description = "사용자1 ID") @RequestParam Long user1Id,
            @Parameter(description = "사용자2 ID") @RequestParam Long user2Id) {
        try {
            // 두 사용자를 강제로 매칭
            String roomId = chatRoomService.createChatRoom(user1Id, user2Id);

            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "데모 매칭이 성공했습니다!";
                public final Object demoMatch = new Object() {
                    public final Long user1 = user1Id;
                    public final Long user2 = user2Id;
                    public final String room = roomId;
                    public final double compatibility = 0.85;
                    public final String note = "이것은 테스트용 매칭입니다";
                };
            });
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "데모 매칭 생성 중 오류 발생";
                public final String error = e.getMessage();
            });
        }
    }

    @GetMapping("/demo/websocket-guide")
    @Operation(summary = "📡 WebSocket 연결 가이드", description = "WebSocket 연결 방법 안내")
    public ResponseEntity<?> getWebSocketGuide() {
        return ResponseEntity.ok(new Object() {
            public final String title = "🔥 실시간 매칭 WebSocket 가이드";
            public final Object connectionInfo = new Object() {
                public final String endpoint = "ws://localhost:9090/ws";
                public final String sockJsEndpoint = "http://localhost:9090/ws";
                public final String protocol = "STOMP";
            };
            public final Object subscriptions = new Object() {
                public final String personalNotifications = "/user/queue/notifications";
                public final String matchingResults = "/user/queue/matching-result";
                public final String chatMessages = "/topic/chat/{roomId}";
                public final String systemStatus = "/topic/system-status";
            };
            public final Object sendEndpoints = new Object() {
                public final String requestMatching = "/app/request-matching";
                public final String sendChatMessage = "/app/send-chat-message";
                public final String shareMusicTrack = "/app/share-music";
                public final String ping = "/app/ping";
            };
            public final String documentation = "자세한 사용법은 Swagger UI에서 확인하세요";
        });
    }
}