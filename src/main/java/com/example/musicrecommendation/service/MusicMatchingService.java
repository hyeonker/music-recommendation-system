package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 음악 기반 사용자 매칭 서비스
 */
@Service
@Transactional
public class MusicMatchingService {

    private final UserRepository userRepository;
    private final UserMatchRepository userMatchRepository;
    private final UserSongLikeRepository userSongLikeRepository;
    private final UserPreferenceService userPreferenceService;
    private final MusicSimilarityService musicSimilarityService;

    public MusicMatchingService(UserRepository userRepository,
                                UserMatchRepository userMatchRepository,
                                UserSongLikeRepository userSongLikeRepository,
                                UserPreferenceService userPreferenceService,
                                MusicSimilarityService musicSimilarityService) {
        this.userRepository = userRepository;
        this.userMatchRepository = userMatchRepository;
        this.userSongLikeRepository = userSongLikeRepository;
        this.userPreferenceService = userPreferenceService;
        this.musicSimilarityService = musicSimilarityService;
    }

    /**
     * 사용자에게 매칭 후보들 찾기
     */
    public List<UserMatch> findMatchCandidates(Long userId, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 사용자 취향 프로필 가져오기 (간단화)
        // 일단 모든 사용자를 대상으로 매칭 진행

        // 모든 다른 사용자들과 유사도 계산
        List<User> allUsers = userRepository.findAll();
        List<UserMatch> candidates = new ArrayList<>();

        for (User otherUser : allUsers) {
            if (otherUser.getId().equals(userId)) continue;

            // 이미 매칭된 사용자는 제외
            Optional<UserMatch> existingMatch = userMatchRepository.findMatchBetweenUsers(userId, otherUser.getId());
            if (existingMatch.isPresent()) continue;

            // 유사도 계산 (간단화)
            int commonSongs = calculateCommonLikedSongs(userId, otherUser.getId());
            double similarity = commonSongs > 0 ? Math.min(0.9, 0.5 + (commonSongs * 0.1)) : 0.3;
            if (similarity >= 0.5) { // 임계값 이상만
                String reason = generateMatchReason(userId, otherUser.getId(), similarity);
                UserMatch match = new UserMatch(userId, otherUser.getId(), similarity, reason);
                candidates.add(match);
            }
        }

        // 유사도 순으로 정렬하고 제한
        return candidates.stream()
                .sorted((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 매칭 생성 및 저장
     */
    public UserMatch createMatch(Long user1Id, Long user2Id) {
        // 이미 존재하는 매칭인지 확인
        Optional<UserMatch> existingMatch = userMatchRepository.findMatchBetweenUsers(user1Id, user2Id);
        if (existingMatch.isPresent()) {
            return existingMatch.get();
        }

        // 유사도 계산 (간단화)
        double similarity = 0.7; // 임시 고정값

        // 공통 좋아요 곡 수 계산
        int commonSongs = calculateCommonLikedSongs(user1Id, user2Id);
        if (commonSongs > 0) {
            similarity = Math.min(0.9, 0.5 + (commonSongs * 0.1));
        }

        // 매칭 생성
        String reason = generateMatchReason(user1Id, user2Id, similarity);
        UserMatch match = new UserMatch(user1Id, user2Id, similarity, reason);

        // 공통 좋아요 곡 수 계산
        int commonLikedSongs = calculateCommonLikedSongs(user1Id, user2Id);
        match.setCommonLikedSongs(commonLikedSongs);

        return userMatchRepository.save(match);
    }

    /**
     * 사용자 간 유사도 계산 (간단화)
     */
    private double calculateUserSimilarity(UserPreferenceProfile profile1, Long user2Id) {
        // 공통 좋아요 곡 기반 유사도 (profile1은 사용하지 않음)
        int commonSongs = calculateCommonLikedSongs(1L, user2Id); // 임시로 1L 사용
        return commonSongs > 0 ? Math.min(0.9, 0.5 + (commonSongs * 0.1)) : 0.3;
    }

    /**
     * 공통 좋아요 곡 수 계산
     */
    private int calculateCommonLikedSongs(Long user1Id, Long user2Id) {
        // 간단한 쿼리 사용
        List<UserSongLike> allLikes = userSongLikeRepository.findAll();

        Set<Long> user1SongIds = allLikes.stream()
                .filter(like -> like.getUser().getId().equals(user1Id))
                .map(like -> like.getSong().getId())
                .collect(Collectors.toSet());

        Set<Long> user2SongIds = allLikes.stream()
                .filter(like -> like.getUser().getId().equals(user2Id))
                .map(like -> like.getSong().getId())
                .collect(Collectors.toSet());

        user1SongIds.retainAll(user2SongIds);
        return user1SongIds.size();
    }

    /**
     * 매칭 이유 생성
     */
    private String generateMatchReason(Long user1Id, Long user2Id, double similarity) {
        int commonSongs = calculateCommonLikedSongs(user1Id, user2Id);

        if (similarity > 0.8) {
            return String.format("🎵 음악 취향이 %d%% 일치! 공통 좋아요 곡 %d개",
                    (int)(similarity * 100), commonSongs);
        } else if (similarity > 0.6) {
            return String.format("🎶 비슷한 음악 스타일을 선호 (%d%% 유사)",
                    (int)(similarity * 100));
        } else {
            return String.format("🎤 새로운 음악 발견 기회 (%d개 공통곡)", commonSongs);
        }
    }

    /**
     * 매칭 상태 업데이트
     */
    public UserMatch updateMatchStatus(Long matchId, String status) {
        UserMatch match = userMatchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("매칭을 찾을 수 없습니다."));

        match.setMatchStatus(status);
        match.setLastInteractionAt(LocalDateTime.now());

        return userMatchRepository.save(match);
    }

    /**
     * 사용자의 모든 매칭 조회
     */
    public List<UserMatch> getUserMatches(Long userId) {
        return userMatchRepository.findMatchesByUserId(userId);
    }

    /**
     * 사용자의 상위 매칭들 조회
     */
    public List<UserMatch> getTopMatches(Long userId, int limit) {
        return userMatchRepository.findTopMatchesByUserId(userId, limit);
    }
}