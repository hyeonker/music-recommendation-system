package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.RealtimeMatchingService;
import org.springframework.messaging.handler.annotation.MessageMapping;

import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import java.time.LocalDateTime;
import java.security.Principal;

/**
 * WebSocket 메시지 처리 컨트롤러
 */
@Controller
public class WebSocketController {

    private final RealtimeMatchingService realtimeMatchingService;

    public WebSocketController(RealtimeMatchingService realtimeMatchingService) {
        this.realtimeMatchingService = realtimeMatchingService;
    }

    /**
     * 클라이언트에서 매칭 요청 메시지 처리
     */
    @MessageMapping("/request-matching")
    @SendToUser("/queue/matching-result")
    public Object handleMatchingRequest(Object request, Principal principal) {
        try {
            // 사용자 ID 추출 (실제로는 Principal에서 가져옴)
            Long userId = extractUserIdFromPrincipal(principal);

            // 비동기 매칭 프로세스 시작
            realtimeMatchingService.processAsyncMatching(userId);

            return new Object() {
                public final String type = "MATCHING_REQUEST_RECEIVED";
                public final String message = "🔍 매칭을 시작합니다...";
                public final Long requestedUserId = userId;
                public final String status = "PROCESSING";
                public final String timestamp = LocalDateTime.now().toString();
            };
        } catch (Exception e) {
            return new Object() {
                public final String type = "MATCHING_ERROR";
                public final String message = "⚠️ 매칭 요청 처리 중 오류 발생";
                public final String error = e.getMessage();
                public final String timestamp = LocalDateTime.now().toString();
            };
        }
    }

    /**
     * 추천곡 업데이트 요청 처리
     */
    @MessageMapping("/request-recommendations")
    @SendToUser("/queue/recommendations")
    public Object handleRecommendationRequest(Object request, Principal principal) {
        try {
            Long userId = extractUserIdFromPrincipal(principal);

            // 추천곡 생성 시뮬레이션
            Object recommendations = generateMockRecommendations();

            // 실시간 추천곡 알림 전송
            realtimeMatchingService.sendRecommendationUpdateNotification(userId, recommendations);

            return new Object() {
                public final String type = "RECOMMENDATIONS_GENERATED";
                public final String message = "🎵 새로운 추천곡을 생성했습니다";
                public final Object data = recommendations;
                public final String timestamp = LocalDateTime.now().toString();
            };
        } catch (Exception e) {
            return new Object() {
                public final String type = "RECOMMENDATION_ERROR";
                public final String message = "⚠️ 추천곡 생성 중 오류 발생";
                public final String error = e.getMessage();
                public final String timestamp = LocalDateTime.now().toString();
            };
        }
    }

    /**
     * 채팅 메시지 처리 (매칭된 사용자간 소통)
     */
    @MessageMapping("/send-message")
    @SendTo("/topic/messages")
    public Object handleChatMessage(Object message, Principal principal) {
        return new Object() {
            public final String type = "CHAT_MESSAGE";
            public final String sender = principal != null ? principal.getName() : "Anonymous";
            public final Object messageContent = message;
            public final String timestamp = LocalDateTime.now().toString();
        };
    }

    /**
     * 시스템 상태 요청 처리
     */
    @MessageMapping("/system-status")
    @SendTo("/topic/system-status")
    public Object handleSystemStatusRequest() {
        realtimeMatchingService.broadcastMatchingStatus();

        return new Object() {
            public final String type = "SYSTEM_STATUS_UPDATE";
            public final String message = "📊 시스템 상태를 업데이트했습니다";
            public final boolean online = true;
            public final String timestamp = LocalDateTime.now().toString();
        };
    }

    /**
     * 사용자 연결 확인
     */
    @MessageMapping("/ping")
    @SendToUser("/queue/pong")
    public Object handlePing(Principal principal) {
        return new Object() {
            public final String type = "PONG";
            public final String message = "🏓 연결 상태 양호";
            public final String user = principal != null ? principal.getName() : "Anonymous";
            public final String timestamp = LocalDateTime.now().toString();
        };
    }

    // 헬퍼 메서드들
    private Long extractUserIdFromPrincipal(Principal principal) {
        // 실제로는 Spring Security의 Principal에서 사용자 ID 추출
        // 지금은 시뮬레이션을 위해 랜덤 ID 반환
        if (principal != null && principal.getName() != null) {
            try {
                return Long.parseLong(principal.getName());
            } catch (NumberFormatException e) {
                return 1L; // 기본값
            }
        }
        return (long) (Math.random() * 100) + 1; // 시뮬레이션용 랜덤 ID
    }

    private Object generateMockRecommendations() {
        return new Object() {
            public final String[] songs = {"추천곡 1", "추천곡 2", "추천곡 3"};
            public final String[] artists = {"아티스트 1", "아티스트 2", "아티스트 3"};
            public final String reason = "당신의 음악 취향 분석 결과";
        };
    }
}