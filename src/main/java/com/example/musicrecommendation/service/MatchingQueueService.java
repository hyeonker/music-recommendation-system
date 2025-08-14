package com.example.musicrecommendation.service;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 스마트 매칭 큐 서비스 - 음악 취향 기반 1:1 매칭
 */
@Service
public class MatchingQueueService {

    private final RealtimeMatchingService realtimeMatchingService;
    private final ChatRoomService chatRoomService;

    // 매칭 대기열 (사용자 ID별)
    private final Queue<Long> matchingQueue = new ConcurrentLinkedQueue<>();

    // 현재 매칭 대기 중인 사용자들 (userId -> 대기 시작 시간)
    private final Map<Long, LocalDateTime> waitingUsers = new ConcurrentHashMap<>();

    // 매칭된 사용자들 (userId -> matchedUserId)
    private final Map<Long, Long> matchedUsers = new ConcurrentHashMap<>();

    public MatchingQueueService(RealtimeMatchingService realtimeMatchingService,
                                ChatRoomService chatRoomService) {
        this.realtimeMatchingService = realtimeMatchingService;
        this.chatRoomService = chatRoomService;
    }

    /**
     * 매칭 요청 - 사용자를 매칭 큐에 추가
     */
    public Object requestMatching(Long userId) {
        // 이미 매칭 대기 중인지 확인
        if (waitingUsers.containsKey(userId)) {
            return new Object() {
                public final boolean success = false;
                public final String message = "이미 매칭 대기 중입니다";
                public final String status = "ALREADY_WAITING";
                public final LocalDateTime waitingSince = waitingUsers.get(userId);
            };
        }

        // 이미 매칭된 사용자인지 확인
        if (matchedUsers.containsKey(userId)) {
            Long matchedUserId = matchedUsers.get(userId);
            return new Object() {
                public final boolean success = false;
                public final String message = "이미 매칭된 상태입니다";
                public final String status = "ALREADY_MATCHED";
                public final Long currentMatchId = matchedUserId;
                public final String chatRoomId = "room_" + Math.min(userId, matchedUserId) + "_" + Math.max(userId, matchedUserId);
            };
        }

        // 매칭 큐에 추가
        matchingQueue.offer(userId);
        waitingUsers.put(userId, LocalDateTime.now());

        // 비동기로 매칭 프로세스 시작
        processMatchingAsync();

        return new Object() {
            public final boolean success = true;
            public final String message = "🔍 매칭 대기열에 추가되었습니다";
            public final String status = "WAITING";
            public final int queuePosition = matchingQueue.size();
            public final LocalDateTime startedAt = LocalDateTime.now();
        };
    }

    /**
     * 비동기 매칭 프로세스
     */
    @Async
    public void processMatchingAsync() {
        if (matchingQueue.size() >= 2) {
            // 대기열에서 두 명 추출
            Long user1Id = matchingQueue.poll();
            Long user2Id = matchingQueue.poll();

            if (user1Id != null && user2Id != null) {
                // 음악 취향 유사도 계산
                double compatibilityScore = calculateMusicCompatibility(user1Id, user2Id);

                // 매칭 성공 기준 (유사도 0.6 이상)
                if (compatibilityScore >= 0.6) {
                    createSuccessfulMatch(user1Id, user2Id, compatibilityScore);
                } else {
                    // 유사도가 낮으면 다시 큐에 추가
                    matchingQueue.offer(user1Id);
                    matchingQueue.offer(user2Id);
                }
            }
        }
    }

    /**
     * 매칭 성공 처리
     */
    private void createSuccessfulMatch(Long user1Id, Long user2Id, double compatibilityScore) {
        // 대기 상태에서 제거
        waitingUsers.remove(user1Id);
        waitingUsers.remove(user2Id);

        // 매칭 상태로 변경
        matchedUsers.put(user1Id, user2Id);
        matchedUsers.put(user2Id, user1Id);

        // 채팅방 생성
        String chatRoomId = chatRoomService.createChatRoom(user1Id, user2Id);

        // 매칭 결과 객체 생성
        Object matchResult = createMatchResultObject(user1Id, user2Id, compatibilityScore, chatRoomId);

        // 양쪽 사용자에게 매칭 성공 알림 전송
        realtimeMatchingService.sendMatchingSuccessNotification(user1Id, matchResult);
        realtimeMatchingService.sendMatchingSuccessNotification(user2Id, matchResult);
    }

    /**
     * 음악 취향 유사도 계산
     */
    private double calculateMusicCompatibility(Long user1Id, Long user2Id) {
        try {
            // 시뮬레이션으로 유사도 계산 (실제로는 사용자 음악 데이터 분석)

            // 기본 유사도 계산 (0.3 ~ 0.95 범위)
            double baseCompatibility = 0.3 + (Math.random() * 0.65);

            // 사용자 ID가 비슷하면 유사도 높게 (테스트용)
            if (Math.abs(user1Id - user2Id) <= 5) {
                baseCompatibility += 0.1;
            }

            return Math.min(baseCompatibility, 0.95);
        } catch (Exception e) {
            return 0.5; // 기본값
        }
    }

    /**
     * 매칭 결과 객체 생성
     */
    private Object createMatchResultObject(Long user1Id, Long user2Id, double compatibilityScore, String chatRoomId) {
        return new Object() {
            public final String type = "MATCH_SUCCESS";
            public final String title = "🎵 매칭 성공!";
            public final String message = "음악 취향이 비슷한 사용자와 매칭되었습니다!";
            public final Object matchInfo = new Object() {
                public final Long userId1 = user1Id;
                public final Long userId2 = user2Id;
                public final double compatibility = Math.round(compatibilityScore * 100.0) / 100.0;
                public final String compatibilityText = (int)(compatibilityScore * 100) + "% 유사";
                public final String[] commonInterests = {"K-POP", "팝송", "인디음악"};
                public final String matchReason = "비슷한 음악 장르를 선호합니다";
            };
            public final Object chatRoom = new Object() {
                public final String roomId = chatRoomId;
                public final String roomName = "음악 채팅방";
                public final boolean active = true;
                public final LocalDateTime createdAt = LocalDateTime.now();
            };
            public final String timestamp = LocalDateTime.now().toString();
        };
    }

    /**
     * 매칭 취소
     */
    public Object cancelMatching(Long userId) {
        boolean wasWaiting = waitingUsers.remove(userId) != null;
        boolean wasInQueue = matchingQueue.remove(userId);

        if (wasWaiting || wasInQueue) {
            return new Object() {
                public final boolean success = true;
                public final String message = "매칭 요청이 취소되었습니다";
                public final String status = "CANCELLED";
                public final LocalDateTime cancelledAt = LocalDateTime.now();
            };
        } else {
            return new Object() {
                public final boolean success = false;
                public final String message = "취소할 매칭 요청이 없습니다";
                public final String status = "NOT_WAITING";
            };
        }
    }

    /**
     * 매칭 해제 (채팅 종료)
     */
    public Object endMatch(Long userId) {
        Long matchedUserId = matchedUsers.remove(userId);
        if (matchedUserId != null) {
            matchedUsers.remove(matchedUserId);

            // 채팅방 비활성화
            String chatRoomId = "room_" + Math.min(userId, matchedUserId) + "_" + Math.max(userId, matchedUserId);
            chatRoomService.deactivateChatRoom(chatRoomId);

            // 상대방에게 매칭 종료 알림
            realtimeMatchingService.sendPersonalNotification(
                    matchedUserId,
                    "매칭 종료",
                    "상대방이 채팅을 종료했습니다",
                    new Object() {
                        public final String type = "MATCH_ENDED";
                        public final Long endedByUserId = userId;
                        public final String endedAt = LocalDateTime.now().toString();
                    }
            );

            return new Object() {
                public final boolean success = true;
                public final String message = "매칭이 종료되었습니다";
                public final String status = "ENDED";
                public final Long previousMatchId = matchedUserId;
                public final LocalDateTime endedAt = LocalDateTime.now();
            };
        } else {
            return new Object() {
                public final boolean success = false;
                public final String message = "종료할 매칭이 없습니다";
                public final String status = "NOT_MATCHED";
            };
        }
    }

    /**
     * 현재 매칭 상태 조회
     */
    public Object getMatchingStatus(Long userId) {
        if (matchedUsers.containsKey(userId)) {
            Long matchedUserId = matchedUsers.get(userId);
            String chatRoomId = "room_" + Math.min(userId, matchedUserId) + "_" + Math.max(userId, matchedUserId);

            return new Object() {
                public final String status = "MATCHED";
                public final String message = "현재 매칭된 상태입니다";
                public final Long matchedWith = matchedUserId;
                public final String roomId = chatRoomId;
                public final boolean canChat = true;
            };
        } else if (waitingUsers.containsKey(userId)) {
            LocalDateTime waitingSince = waitingUsers.get(userId);
            return new Object() {
                public final String status = "WAITING";
                public final String message = "매칭 대기 중입니다";
                public final LocalDateTime waitingStarted = waitingSince;
                public final int queuePosition = new ArrayList<>(matchingQueue).indexOf(userId) + 1;
                public final boolean canCancel = true;
            };
        } else {
            return new Object() {
                public final String status = "IDLE";
                public final String message = "매칭 요청을 시작할 수 있습니다";
                public final boolean canRequest = true;
            };
        }
    }

    /**
     * 전체 매칭 시스템 상태
     */
    public Object getSystemStatus() {
        return new Object() {
            public final boolean online = true;
            public final String message = "매칭 시스템 정상 가동 중";
            public final Object statistics = new Object() {
                public final int totalWaiting = waitingUsers.size();
                public final int totalMatched = matchedUsers.size() / 2; // 쌍의 개수
                public final int queueLength = matchingQueue.size();
                public final double avgCompatibility = 0.75; // 평균 유사도
            };
            public final String timestamp = LocalDateTime.now().toString();
        };
    }
}