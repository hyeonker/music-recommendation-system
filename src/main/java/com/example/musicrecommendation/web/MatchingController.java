package com.example.musicrecommendation.web;

import com.example.musicrecommendation.domain.*;
import com.example.musicrecommendation.service.MusicMatchingService;
import com.example.musicrecommendation.web.dto.MatchingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 음악 기반 매칭 시스템 REST API
 */
@RestController
@RequestMapping("/api/matching")
@Tag(name = "🎵 음악 매칭 시스템", description = "음악 취향 기반 사용자 매칭 API")
public class MatchingController {

    private final MusicMatchingService musicMatchingService;
    private final UserRepository userRepository;

    public MatchingController(MusicMatchingService musicMatchingService, UserRepository userRepository) {
        this.musicMatchingService = musicMatchingService;
        this.userRepository = userRepository;
    }

    /**
     * 사용자 매칭 후보 찾기
     */
    @GetMapping("/candidates/{userId}")
    @Operation(summary = "매칭 후보 찾기", description = "음악 취향이 비슷한 사용자 매칭 후보들을 찾습니다")
    public ResponseEntity<MatchingResponse.MatchCandidateResponse> findMatchCandidates(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @Parameter(description = "후보 수 제한") @RequestParam(defaultValue = "10") int limit) {

        List<UserMatch> candidates = musicMatchingService.findMatchCandidates(userId, limit);

        List<MatchingResponse.MatchDto> matchDtos = candidates.stream()
                .map(this::convertToMatchDto)
                .collect(Collectors.toList());

        MatchingResponse.MatchCandidateResponse response =
                new MatchingResponse.MatchCandidateResponse(userId, matchDtos);

        return ResponseEntity.ok(response);
    }

    /**
     * 매칭 생성
     */
    @PostMapping("/create")
    @Operation(summary = "매칭 생성", description = "두 사용자 간 매칭을 생성합니다")
    public ResponseEntity<MatchingResponse.MatchDto> createMatch(
            @Parameter(description = "사용자 1 ID") @RequestParam Long user1Id,
            @Parameter(description = "사용자 2 ID") @RequestParam Long user2Id) {

        UserMatch match = musicMatchingService.createMatch(user1Id, user2Id);
        MatchingResponse.MatchDto matchDto = convertToMatchDto(match);

        return ResponseEntity.ok(matchDto);
    }

    /**
     * 사용자의 모든 매칭 조회
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "사용자 매칭 목록", description = "사용자의 모든 매칭을 조회합니다")
    public ResponseEntity<MatchingResponse.MatchCandidateResponse> getUserMatches(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {

        List<UserMatch> matches = musicMatchingService.getUserMatches(userId);

        List<MatchingResponse.MatchDto> matchDtos = matches.stream()
                .map(this::convertToMatchDto)
                .collect(Collectors.toList());

        MatchingResponse.MatchCandidateResponse response =
                new MatchingResponse.MatchCandidateResponse(userId, matchDtos);

        return ResponseEntity.ok(response);
    }

    /**
     * 상위 매칭들 조회
     */
    @GetMapping("/top/{userId}")
    @Operation(summary = "상위 매칭 조회", description = "사용자의 상위 매칭들을 조회합니다")
    public ResponseEntity<MatchingResponse.MatchCandidateResponse> getTopMatches(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @Parameter(description = "조회할 매칭 수") @RequestParam(defaultValue = "5") int limit) {

        List<UserMatch> topMatches = musicMatchingService.getTopMatches(userId, limit);

        List<MatchingResponse.MatchDto> matchDtos = topMatches.stream()
                .map(this::convertToMatchDto)
                .collect(Collectors.toList());

        MatchingResponse.MatchCandidateResponse response =
                new MatchingResponse.MatchCandidateResponse(userId, matchDtos);

        return ResponseEntity.ok(response);
    }

    /**
     * 매칭 상태 업데이트
     */
    @PutMapping("/status/{matchId}")
    @Operation(summary = "매칭 상태 변경", description = "매칭의 상태를 변경합니다")
    public ResponseEntity<MatchingResponse.MatchDto> updateMatchStatus(
            @Parameter(description = "매칭 ID") @PathVariable Long matchId,
            @Parameter(description = "새로운 상태 (PENDING, ACCEPTED, REJECTED)") @RequestParam String status) {

        UserMatch updatedMatch = musicMatchingService.updateMatchStatus(matchId, status);
        MatchingResponse.MatchDto matchDto = convertToMatchDto(updatedMatch);

        return ResponseEntity.ok(matchDto);
    }

    /**
     * 매칭 통계
     */
    @GetMapping("/stats")
    @Operation(summary = "매칭 통계", description = "전체 매칭 시스템 통계를 조회합니다")
    public ResponseEntity<?> getMatchingStats() {
        // 간단한 통계 정보
        return ResponseEntity.ok(new Object() {
            public final String message = "🎵 매칭 시스템이 정상 작동 중입니다!";
            public final String description = "음악 취향 기반 사용자 매칭 서비스";
            public final String[] features = {
                    "📊 유사도 기반 매칭",
                    "🎼 공통 좋아요 곡 분석",
                    "💕 실시간 매칭 후보 추천",
                    "⚡ 매칭 상태 관리"
            };
        });
    }

    /**
     * UserMatch를 MatchDto로 변환
     */
    private MatchingResponse.MatchDto convertToMatchDto(UserMatch match) {
        // 상대방 사용자 정보 가져오기 (임시로 간단화)
        MatchingResponse.UserInfo userInfo = new MatchingResponse.UserInfo(
                match.getUser2Id(),
                "User " + match.getUser2Id(),
                "user" + match.getUser2Id() + "@example.com",
                match.getCreatedAt()
        );

        MatchingResponse.MatchDto dto = new MatchingResponse.MatchDto(
                match.getId(),
                userInfo,
                match.getSimilarityScore(),
                match.getMatchStatus(),
                match.getMatchReason(),
                match.getCommonLikedSongs()
        );

        dto.setCommonArtists(match.getCommonArtists());
        dto.setCreatedAt(match.getCreatedAt());
        dto.setLastInteractionAt(match.getLastInteractionAt());

        return dto;
    }
}