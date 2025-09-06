package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.NotificationService;
import com.example.musicrecommendation.service.NotificationService.NotificationData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * 사용자의 알림 목록 조회
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        
        try {
            List<NotificationData> notifications = notificationService.getUserNotifications(userId, limit, unreadOnly);
            int unreadCount = notificationService.getUnreadCount(userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "userId", userId,
                "notifications", notifications,
                "unreadCount", unreadCount,
                "totalCount", notifications.size(),
                "timestamp", java.time.Instant.now().toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "알림 조회 실패: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            ));
        }
    }
    
    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/{userId}/count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@PathVariable Long userId) {
        try {
            int unreadCount = notificationService.getUnreadCount(userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "userId", userId,
                "unreadCount", unreadCount,
                "timestamp", java.time.Instant.now().toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "알림 개수 조회 실패: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            ));
        }
    }
    
    /**
     * 특정 알림을 읽음으로 표시
     */
    @PutMapping("/{userId}/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long userId,
            @PathVariable String notificationId) {
        
        try {
            boolean success = notificationService.markAsRead(userId, notificationId);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "알림이 읽음 처리되었습니다",
                    "notificationId", notificationId,
                    "unreadCount", notificationService.getUnreadCount(userId),
                    "timestamp", java.time.Instant.now().toString()
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "알림 읽음 처리 실패: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            ));
        }
    }
    
    /**
     * 모든 알림을 읽음으로 표시
     */
    @PutMapping("/{userId}/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@PathVariable Long userId) {
        try {
            int markedCount = notificationService.markAllAsRead(userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "모든 알림이 읽음 처리되었습니다",
                "markedCount", markedCount,
                "unreadCount", 0,
                "timestamp", java.time.Instant.now().toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "알림 읽음 처리 실패: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            ));
        }
    }
    
    /**
     * 알림 구독 설정 조회
     */
    @GetMapping("/{userId}/subscriptions")
    public ResponseEntity<Map<String, Object>> getUserSubscriptions(@PathVariable Long userId) {
        try {
            Set<String> subscriptions = notificationService.getUserSubscriptions(userId);
            
            // 사용 가능한 알림 유형 목록
            Map<String, String> availableTypes = Map.of(
                "MATCH_FOUND", "새로운 매칭",
                "NEW_MESSAGE", "새 메시지",
                "PLAYLIST_SHARED", "플레이리스트 공유",
                "NEW_RECOMMENDATIONS", "음악 추천",
                "BROADCAST", "공지사항"
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "userId", userId,
                "subscriptions", subscriptions,
                "availableTypes", availableTypes,
                "timestamp", java.time.Instant.now().toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "구독 설정 조회 실패: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            ));
        }
    }
    
    /**
     * 알림 구독 설정
     */
    @PostMapping("/{userId}/subscriptions")
    public ResponseEntity<Map<String, Object>> updateSubscription(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> request) {
        
        try {
            String notificationType = (String) request.get("notificationType");
            Boolean subscribe = (Boolean) request.get("subscribe");
            
            if (notificationType == null || subscribe == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "notificationType과 subscribe 필드가 필요합니다",
                    "timestamp", java.time.Instant.now().toString()
                ));
            }
            
            if (subscribe) {
                notificationService.subscribe(userId, notificationType);
            } else {
                notificationService.unsubscribe(userId, notificationType);
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", subscribe ? "알림을 구독했습니다" : "알림 구독을 해제했습니다",
                "notificationType", notificationType,
                "subscribed", subscribe,
                "timestamp", java.time.Instant.now().toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "구독 설정 변경 실패: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            ));
        }
    }
    
    
    
    /**
     * 브로드캐스트 알림 전송 (관리자 전용)
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> sendBroadcastNotification(@RequestBody Map<String, Object> request) {
        try {
            String title = (String) request.get("title");
            String message = (String) request.get("message");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) request.getOrDefault("data", new HashMap<>());
            
            if (title == null || message == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "title과 message 필드가 필요합니다",
                    "timestamp", java.time.Instant.now().toString()
                ));
            }
            
            notificationService.broadcastNotification(title, message, data);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "브로드캐스트 알림이 전송되었습니다",
                "title", title,
                "timestamp", java.time.Instant.now().toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "브로드캐스트 알림 전송 실패: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            ));
        }
    }
}