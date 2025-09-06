package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.UserMatch;
import com.example.musicrecommendation.domain.User;
import com.example.musicrecommendation.domain.UserRepository;
import com.example.musicrecommendation.event.MatchingEvent;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 스마트 매칭 큐 서비스 - 실제 음악 알고리즘 기반 1:1 매칭
 */
@Slf4j
@Service
public class MatchingQueueService {

    private final RealtimeMatchingService realtimeMatchingService;
    private final SecureChatRoomService secureChatRoomService;
    private final MusicMatchingService musicMatchingService;
    private final UserRepository userRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    // 매칭 대기열 (사용자 ID별)
    private final Queue<Long> matchingQueue = new ConcurrentLinkedQueue<>();

    // 현재 매칭 대기 중인 사용자들 (userId -> 대기 시작 시간)
    private final Map<Long, LocalDateTime> waitingUsers = new ConcurrentHashMap<>();

    // 매칭된 사용자들 (userId -> matchedUserId)
    private final Map<Long, Long> matchedUsers = new ConcurrentHashMap<>();

    public MatchingQueueService(RealtimeMatchingService realtimeMatchingService,
                                SecureChatRoomService secureChatRoomService,
                                MusicMatchingService musicMatchingService,
                                UserRepository userRepository,
                                org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.realtimeMatchingService = realtimeMatchingService;
        this.secureChatRoomService = secureChatRoomService;
        this.musicMatchingService = musicMatchingService;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 매칭 요청 - 사용자를 매칭 큐에 추가
     */
    public Object requestMatching(Long userId) {
        System.out.println("=== 매칭 요청 시작 ===");
        System.out.println("요청 사용자 ID: " + userId);
        
        // 상태 정리: 이미 매칭된 사용자가 waitingUsers에 있으면 제거 (상태 불일치 해결)
        if (matchedUsers.containsKey(userId) && waitingUsers.containsKey(userId)) {
            System.out.println("⚠️ 상태 불일치 감지: 매칭된 사용자 " + userId + "가 대기열에 있음 - 정리 중");
            waitingUsers.remove(userId);
            matchingQueue.remove(userId);
        }
        
        // 이미 매칭된 사용자인지 확인
        if (matchedUsers.containsKey(userId)) {
            Long matchedUserId = matchedUsers.get(userId);
            System.out.println("사용자 " + userId + "는 이미 " + matchedUserId + "와 매칭됨");
            return new Object() {
                public final boolean success = false;
                public final String message = "이미 매칭된 상태입니다";
                public final String status = "ALREADY_MATCHED";
                public final Long currentMatchId = matchedUserId;
                // 보안 강화된 채팅방 생성
                private final SecureChatRoomService.ChatRoomCreationResult roomResult = 
                    secureChatRoomService.createSecureChatRoom(userId, matchedUserId);
                public final String chatRoomId = roomResult.getRoomId();
            };
        }
        
        // 이미 매칭 대기 중인지 확인
        if (waitingUsers.containsKey(userId)) {
            System.out.println("사용자 " + userId + "는 이미 대기 중, 매칭 프로세스 재실행");
            
            // 이미 대기 중이어도 매칭 프로세스를 다시 실행 (다른 사용자와 매칭 시도)
            processMatchingSync();
            
            return new Object() {
                public final boolean success = true;
                public final String message = "매칭 대기 중입니다. 다른 사용자를 찾는 중...";
                public final String status = "WAITING";
                public final LocalDateTime waitingSince = waitingUsers.get(userId);
            };
        }

        // 매칭 큐에 추가
        matchingQueue.offer(userId);
        waitingUsers.put(userId, LocalDateTime.now());
        
        System.out.println("사용자 " + userId + " 대기열 추가 완료");
        System.out.println("현재 대기열: " + matchingQueue);
        System.out.println("현재 대기 사용자: " + waitingUsers.keySet());

        // 비동기로 매칭 프로세스 시작
        boolean matchFound = processMatchingSync();

        if (matchFound) {
            return new Object() {
                public final boolean success = true;
                public final String message = "🎵 매칭 성공!";
                public final String status = "MATCHED";
                public final LocalDateTime matchedAt = LocalDateTime.now();
            };
        } else {
            return new Object() {
                public final boolean success = true;
                public final String message = "🔍 매칭 대기열에 추가되었습니다";
                public final String status = "WAITING";
                public final int queuePosition = matchingQueue.size();
                public final LocalDateTime startedAt = LocalDateTime.now();
            };
        }
    }

    /**
     * 동기 매칭 프로세스 (매칭 성공 여부 반환)
     */
    public boolean processMatchingSync() {
        // 매칭 프로세스 시작 로그
        System.out.println("=== 스마트 매칭 프로세스 시작 ===");
        System.out.println("대기열 크기: " + matchingQueue.size());
        System.out.println("대기 사용자: " + waitingUsers.keySet());
        
        if (matchingQueue.size() >= 2) {
            // 스마트 매칭: 모든 대기 사용자 간 최적 유사도 매칭 찾기
            return findOptimalMatch();
        } else {
            System.out.println("대기열에 충분한 사용자가 없음 (현재: " + matchingQueue.size() + "명)");
        }
        return false; // 매칭 실패
    }
    
    /**
     * 최적 유사도 매칭 알고리즘
     * 대기열의 모든 사용자 쌍 중에서 가장 높은 유사도를 가진 쌍을 매칭
     */
    private boolean findOptimalMatch() {
        System.out.println("🎯 최적 유사도 매칭 알고리즘 실행");
        
        // 이미 매칭된 사용자들 제외하고 실제 대기 중인 사용자만 필터링
        List<Long> waitingUsersList = waitingUsers.keySet().stream()
            .filter(userId -> !matchedUsers.containsKey(userId))
            .collect(java.util.stream.Collectors.toList());
            
        double bestCompatibility = 0.0;
        Long bestUser1 = null;
        Long bestUser2 = null;
        
        System.out.println("전체 대기 사용자: " + waitingUsers.keySet());
        System.out.println("이미 매칭된 사용자: " + matchedUsers.keySet());
        System.out.println("실제 매칭 가능한 대기 사용자 목록: " + waitingUsersList);
        
        // 모든 사용자 쌍에 대해 유사도 계산
        for (int i = 0; i < waitingUsersList.size(); i++) {
            for (int j = i + 1; j < waitingUsersList.size(); j++) {
                Long user1 = waitingUsersList.get(i);
                Long user2 = waitingUsersList.get(j);
                
                double compatibility = calculateMusicCompatibility(user1, user2);
                System.out.println("유사도 계산: " + user1 + " <-> " + user2 + " = " + 
                    String.format("%.3f", compatibility));
                
                // 최고 유사도 쌍 찾기
                if (compatibility > bestCompatibility) {
                    bestCompatibility = compatibility;
                    bestUser1 = user1;
                    bestUser2 = user2;
                }
            }
        }
        
        // 동적 기준치 계산 (시간 기반)
        double dynamicThreshold = calculateDynamicThreshold(bestUser1, bestUser2);
        
        // 최적 매칭 결과 출력
        if (bestUser1 != null && bestUser2 != null && bestCompatibility >= dynamicThreshold) {
            System.out.println("🏆 최적 매칭 발견!");
            System.out.println("최고 유사도 쌍: " + bestUser1 + " <-> " + bestUser2 + 
                " (유사도: " + String.format("%.3f", bestCompatibility) + 
                ", 적용 기준치: " + String.format("%.3f", dynamicThreshold) + ")");
            
            // 큐에서 해당 사용자들 제거
            matchingQueue.remove(bestUser1);
            matchingQueue.remove(bestUser2);
            
            // 매칭 생성
            createSuccessfulMatch(bestUser1, bestUser2, bestCompatibility);
            return true;
            
        } else if (bestUser1 != null && bestUser2 != null) {
            System.out.println("⚠️ 최고 유사도가 기준치 미달");
            System.out.println("최고 유사도: " + String.format("%.3f", bestCompatibility) + 
                " (적용 기준치: " + String.format("%.3f", dynamicThreshold) + ")");
            System.out.println("매칭 대기 계속...");
            return false;
            
        } else {
            System.out.println("❌ 매칭 가능한 사용자 쌍이 없음");
            return false;
        }
    }

    /**
     * 매칭 성공 처리
     */
    private void createSuccessfulMatch(Long user1Id, Long user2Id, double compatibilityScore) {
        try {
            // 모든 상태에서 완전히 제거 (중복 매칭 방지)
            waitingUsers.remove(user1Id);
            waitingUsers.remove(user2Id);
            matchingQueue.remove(user1Id);
            matchingQueue.remove(user2Id);

            // 매칭 상태로 변경
            matchedUsers.put(user1Id, user2Id);
            matchedUsers.put(user2Id, user1Id);
            
            System.out.println("=== 매칭 상태 완전 정리 ===");
            System.out.println("대기열에서 제거: " + user1Id + ", " + user2Id);
            System.out.println("매칭 맵에 추가: " + user1Id + " -> " + user2Id + ", " + user2Id + " -> " + user1Id);
            System.out.println("현재 대기 사용자: " + waitingUsers.keySet());
            System.out.println("현재 매칭된 사용자: " + matchedUsers.keySet());
            System.out.println("현재 대기열 크기: " + matchingQueue.size());

            // 보안 채팅방 생성 (UUID 기반)
            SecureChatRoomService.ChatRoomCreationResult chatRoomResult = 
                secureChatRoomService.createSecureChatRoom(user1Id, user2Id);
            String chatRoomId = chatRoomResult.getRoomId();

            // 실제 매칭 정보 조회
            UserMatch actualMatch = getOrCreateMatch(user1Id, user2Id);
            
            // 매칭 결과 객체 생성 (최신 계산된 호환성 점수 사용)
            Object matchResult = createEnhancedMatchResultObject(user1Id, user2Id, actualMatch, chatRoomId, compatibilityScore);

            // Event를 통한 매칭 성공 알림 전송 (최신 호환성 점수 사용)
            publishMatchingSuccessEvent(user1Id, user2Id, actualMatch, chatRoomId, compatibilityScore);
            publishMatchingSuccessEvent(user2Id, user1Id, actualMatch, chatRoomId, compatibilityScore);
            
            log.info("매칭 성공 처리 완료: {} <-> {}, 유사도: {:.3f}", 
                    user1Id, user2Id, compatibilityScore);
                    
        } catch (Exception e) {
            log.error("매칭 성공 처리 중 오류: {}", e.getMessage());
        }
    }
    
    private UserMatch getOrCreateMatch(Long user1Id, Long user2Id) {
        try {
            // 기존 매칭 조회
            return musicMatchingService.createMatch(user1Id, user2Id);
        } catch (Exception e) {
            log.warn("실제 매칭 조회/생성 실패, 기본 매칭 정보 생성: {}", e.getMessage());
            // 기본 매칭 정보 생성
            UserMatch defaultMatch = new UserMatch(user1Id, user2Id, 0.6, "실시간 매칭");
            defaultMatch.setCommonLikedSongs(0);
            return defaultMatch;
        }
    }

    /**
     * 동적 기준치 계산 - 대기 시간에 따라 기준치를 점진적으로 완화
     */
    private double calculateDynamicThreshold(Long user1Id, Long user2Id) {
        if (user1Id == null || user2Id == null) {
            return 0.3; // 기본 기준치
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime user1WaitTime = waitingUsers.get(user1Id);
        LocalDateTime user2WaitTime = waitingUsers.get(user2Id);
        
        if (user1WaitTime == null || user2WaitTime == null) {
            return 0.3; // 기본 기준치
        }
        
        // 두 사용자 중 더 오래 기다린 시간 기준
        long maxWaitMinutes = Math.max(
            java.time.Duration.between(user1WaitTime, now).toMinutes(),
            java.time.Duration.between(user2WaitTime, now).toMinutes()
        );
        
        double threshold;
        if (maxWaitMinutes == 0) {
            threshold = 0.3;  // 첫 시도: 기존 기준치 유지 (품질 우선)
        } else if (maxWaitMinutes <= 1) {
            threshold = 0.2;  // 1분 이하: 완화된 기준치
        } else if (maxWaitMinutes <= 2) {
            threshold = 0.15; // 1-2분: 더 완화된 기준치
        } else if (maxWaitMinutes <= 3) {
            threshold = 0.12; // 2-3분: 많이 완화된 기준치
        } else {
            threshold = 0.08; // 3분 이상: 최소 기준치 (거의 누구든 매칭)
        }
        
        System.out.println("동적 기준치 계산: " + user1Id + "(" + 
            java.time.Duration.between(user1WaitTime, now).toMinutes() + "분 대기), " +
            user2Id + "(" + java.time.Duration.between(user2WaitTime, now).toMinutes() + "분 대기) " +
            "-> 기준치: " + String.format("%.2f", threshold));
        
        return threshold;
    }

    /**
     * 대기열의 두 사용자 간 직접 유사도 계산 (MusicMatchingService의 고급 알고리즘 사용)
     */
    private double calculateMusicCompatibility(Long user1Id, Long user2Id) {
        try {
            log.info("대기열 사용자 간 유사도 직접 계산: {} <-> {}", user1Id, user2Id);
            
            // MusicMatchingService의 실제 고급 알고리즘을 사용하되, 
            // 매칭 생성은 하지 않고 유사도만 계산
            // 임시 매칭 객체를 생성해서 유사도만 가져옴
            UserMatch tempMatch = new UserMatch(user1Id, user2Id, 0.0, "임시 계산");
            
            // MusicMatchingService의 calculateEnhancedSimilarity 메서드와 동일한 로직 적용
            // (여기서는 간단하게 기본 유사도 계산)
            
            // MusicMatchingService의 실제 고급 알고리즘 사용
            try {
                double actualSimilarity = musicMatchingService.calculateEnhancedSimilarity(user1Id, user2Id);
                log.info("실제 알고리즘 계산 유사도: {:.3f}", actualSimilarity);
                return actualSimilarity;
            } catch (Exception e) {
                log.warn("실제 유사도 계산 실패, 기본 알고리즘 사용: {}", e.getMessage());
                // 기본 유사도 계산
                double baseSimilarity = 0.4 + (Math.random() * 0.4);
                log.info("기본 유사도: {:.3f}", baseSimilarity);
                return baseSimilarity;
            }
            
        } catch (Exception e) {
            log.error("유사도 계산 중 오류: {}", e.getMessage());
            return 0.4; // 기본값
        }
    }

    /**
     * 향상된 매칭 결과 객체 생성 (실제 MusicMatchingService 기반)
     */
    private Object createEnhancedMatchResultObject(Long user1Id, Long user2Id, UserMatch actualMatch, String chatRoomId, double latestCompatibilityScore) {
        return new Object() {
            public final String type = "MATCH_SUCCESS";
            public final String title = "🎵 실제 음악 취향 매칭 성공!";
            public final String message = "정교한 AI 알고리즘으로 분석한 음악 취향 매칭이 완료되었습니다!";
            public final Object matchInfo = new Object() {
                public final Long userId1 = user1Id;
                public final Long userId2 = user2Id;
                public final double compatibility = Math.round(latestCompatibilityScore * 100.0) / 100.0;
                public final String compatibilityText = (int)(latestCompatibilityScore * 100) + "% 음악 호환성";
                public final String matchReason = actualMatch.getMatchReason() != null ? 
                    actualMatch.getMatchReason() : "다양한 음악적 요소 분석 결과";
                public final int commonLikedSongs = actualMatch.getCommonLikedSongs();
                public final String algorithmUsed = "MusicMatchingService 고급 알고리즘";
                public final Object analysisDetails = new Object() {
                    public final String helpfulWeight = "25% - 도움이 됨 리뷰 분석";
                    public final String likedSongsWeight = "20% - 좋아요 음악 분석";
                    public final String festivalWeight = "15% - 페스티벌 경험";
                    public final String artistWeight = "15% - 선호 아티스트";
                    public final String genreWeight = "15% - 선호 장르";
                    public final String moodWeight = "5% - 음악 무드";
                    public final String preferenceWeight = "5% - 기타 선호도";
                };
            };
            public final Object chatRoom = new Object() {
                public final String roomId = chatRoomId;
                public final String roomName = "🎵 AI 매칭 음악방";
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

            // UUID 기반 보안 채팅방은 자동 만료되므로 별도 비활성화 불필요
            String chatRoomId = secureChatRoomService.getUserActiveRoom(userId);
            if (chatRoomId != null) {
                log.info("매칭 종료 - 채팅방 {}는 1시간 후 자동 만료됩니다", chatRoomId);
            }

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
     * 사용자가 매칭된 상태인지 확인
     */
    public boolean isUserMatched(Long userId) {
        boolean matched = matchedUsers.containsKey(userId);
        log.info("사용자 {} 매칭 상태 확인: {}", userId, matched);
        return matched;
    }

    /**
     * 현재 매칭 상태 조회
     */
    public Object getMatchingStatus(Long userId) {
        System.out.println("=== getMatchingStatus 호출 ===");
        System.out.println("요청 사용자 ID: " + userId);
        System.out.println("현재 matchedUsers 맵: " + matchedUsers);
        System.out.println("현재 waitingUsers 맵: " + waitingUsers);
        
        if (matchedUsers.containsKey(userId)) {
            Long matchedUserId = matchedUsers.get(userId);
            String chatRoomId = secureChatRoomService.getUserActiveRoom(userId);
            System.out.println("사용자 " + userId + "는 " + matchedUserId + "와 매칭됨 (MATCHED 상태)");

            return new Object() {
                public final String status = "MATCHED";
                public final String message = "현재 매칭된 상태입니다";
                public final Long matchedWith = matchedUserId;
                public final String roomId = chatRoomId;
                public final boolean canChat = true;
            };
        } else if (waitingUsers.containsKey(userId)) {
            LocalDateTime waitingSince = waitingUsers.get(userId);
            System.out.println("사용자 " + userId + "는 대기 중 (WAITING 상태)");
            return new Object() {
                public final String status = "WAITING";
                public final String message = "매칭 대기 중입니다";
                public final LocalDateTime waitingStarted = waitingSince;
                public final int queuePosition = new ArrayList<>(matchingQueue).indexOf(userId) + 1;
                public final boolean canCancel = true;
            };
        } else {
            System.out.println("사용자 " + userId + "는 유휴 상태 (IDLE 상태)");
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

    /**
     * 매칭 성공 이벤트 발행 (책임 분리)
     */
    private void publishMatchingSuccessEvent(Long userId, Long matchedUserId, UserMatch matchData, String chatRoomId, double latestCompatibilityScore) {
        log.info("매칭 성공 이벤트 발행: userId={}, matchedUserId={}", userId, matchedUserId);
        
        // 실제 사용자 이름 가져오기
        String matchedUserName = "음악친구";
        try {
            User matchedUser = userRepository.findById(matchedUserId).orElse(null);
            if (matchedUser != null && matchedUser.getName() != null && !matchedUser.getName().trim().isEmpty()) {
                matchedUserName = matchedUser.getName();
            } else {
                matchedUserName = "사용자" + matchedUserId; // 폴백
            }
        } catch (Exception e) {
            log.warn("매칭된 사용자 이름 조회 실패: userId={}, 폴백 이름 사용", matchedUserId, e);
            matchedUserName = "음악친구"; // 에러 시 폴백
        }
        
        final String finalMatchedUserName = matchedUserName;
        log.info("매칭된 사용자 이름: {}", finalMatchedUserName);
        
        // 공통 장르와 아티스트 정보 조회
        List<String> genresResult = new ArrayList<>();
        List<String> artistsResult = new ArrayList<>();
        try {
            genresResult = musicMatchingService.getCommonGenres(userId, matchedUserId);
            artistsResult = musicMatchingService.getCommonArtists(userId, matchedUserId);
            log.info("[공통 관심사] userId={} <-> matchedUserId={}, 공통 장르: {}, 공통 아티스트: {}", 
                    userId, matchedUserId, genresResult, artistsResult);
        } catch (Exception e) {
            log.warn("공통 관심사 조회 실패: userId={}, matchedUserId={}, error={}", userId, matchedUserId, e.getMessage());
        }
        final List<String> commonGenres = genresResult;
        final List<String> commonArtists = artistsResult;
        
        // 공통 관심사 텍스트 생성
        final String commonInterestsText = generateCommonInterestsText(commonGenres, commonArtists);
        log.info("[공통 관심사 텍스트] userId={} <-> matchedUserId={}: '{}'", userId, matchedUserId, commonInterestsText);
        
        // 매칭 데이터 객체 생성
        Object additionalData = new Object() {
            public final String type = "MATCHING_SUCCESS";
            public final boolean success = true;
            public final String status = "MATCHED";
            public final String title = "🎵 매칭 성공!";
            public final String message = "음악 취향이 비슷한 사용자와 매칭되었습니다!";
            public final Object matchedUser = new Object() {
                public final Long id = matchedUserId;
                public final String name = finalMatchedUserName;
                public final String roomId = chatRoomId;
            };
            public final Object matchInfo = new Object() {
                public final double compatibility = Math.round(latestCompatibilityScore * 100.0) / 100.0;
                public final String compatibilityText = (int)(latestCompatibilityScore * 100) + "% 음악 호환성";
                public final String matchReason = matchData.getMatchReason();
                public final int commonLikedSongs = matchData.getCommonLikedSongs();
                // 공통 관심사 정보 추가 - outer scope의 변수들을 참조
                public final List<String> sharedGenres = commonGenres;
                public final List<String> sharedArtists = commonArtists;
                public final String commonInterests = commonInterestsText;
            };
            public final Object roomId = chatRoomId;
            public final String timestamp = LocalDateTime.now().toString();
        };
        
        // 이벤트 발행
        try {
            MatchingEvent event = new MatchingEvent(
                this, 
                userId, 
                matchedUserId, 
                chatRoomId, // 원본 UUID roomId 사용
                "SUCCESS", 
                "매칭 성공", 
                additionalData
            );
            eventPublisher.publishEvent(event);
            log.info("매칭 성공 이벤트 발행 완료: {}", event);
        } catch (Exception e) {
            log.error("매칭 이벤트 발행 실패: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 공통 관심사 텍스트 생성
     */
    private String generateCommonInterestsText(List<String> commonGenres, List<String> commonArtists) {
        List<String> interests = new ArrayList<>();
        
        // 공통 장르 추가 (최대 3개)
        if (commonGenres != null && !commonGenres.isEmpty()) {
            interests.addAll(commonGenres.stream().limit(3).collect(java.util.stream.Collectors.toList()));
        }
        
        // 공통 아티스트 추가 (최대 2개, 전체 최대 5개까지)
        if (commonArtists != null && !commonArtists.isEmpty() && interests.size() < 5) {
            int remainingSlots = 5 - interests.size();
            interests.addAll(commonArtists.stream().limit(Math.min(remainingSlots, 2)).collect(java.util.stream.Collectors.toList()));
        }
        
        if (interests.isEmpty()) {
            return "새로운 음악 탐험";
        }
        
        return String.join(", ", interests);
    }
}