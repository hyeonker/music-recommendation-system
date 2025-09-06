package com.example.musicrecommendation.web;

import com.example.musicrecommendation.domain.MusicSharingHistory;
import com.example.musicrecommendation.service.MusicSharingHistoryService;
import com.example.musicrecommendation.service.OAuth2UserResolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}, allowCredentials = "true")
@RequestMapping("/api/music-sharing-history")
@Tag(name = "🎵 Music Sharing History", description = "음악 공유 히스토리 관리 API")
public class MusicSharingHistoryController {
    
    private final MusicSharingHistoryService musicSharingHistoryService;
    private final OAuth2UserResolutionService userResolutionService;
    
    public MusicSharingHistoryController(MusicSharingHistoryService musicSharingHistoryService,
                                       OAuth2UserResolutionService userResolutionService) {
        this.musicSharingHistoryService = musicSharingHistoryService;
        this.userResolutionService = userResolutionService;
    }
    
    /**
     * 현재 사용자 ID 추출 (OAuth2 또는 로컬 인증)
     * OAuth2UserResolutionService를 사용하여 데이터베이스에서 실제 사용자 ID 조회
     */
    private Long getCurrentUserId(OAuth2User oAuth2User) {
        System.out.println("[MusicSharingHistoryController] getCurrentUserId 호출됨");
        
        if (oAuth2User == null) {
            System.out.println("[MusicSharingHistoryController] oAuth2User가 null임");
            throw new IllegalStateException("인증이 필요합니다");
        }
        
        System.out.println("[MusicSharingHistoryController] OAuth2User attributes: " + oAuth2User.getAttributes());
        System.out.println("[MusicSharingHistoryController] Provider info: " + userResolutionService.getProviderInfo(oAuth2User));
        
        Long userId = userResolutionService.resolveUserId(oAuth2User);
        
        if (userId == null) {
            System.err.println("[MusicSharingHistoryController] OAuth2UserResolutionService에서 사용자 ID를 찾을 수 없음");
            throw new IllegalStateException("사용자 정보를 찾을 수 없습니다. 다시 로그인해 주세요.");
        }
        
        System.out.println("[MusicSharingHistoryController] 사용자 ID 조회 성공: " + userId);
        return userId;
    }
    
    @GetMapping
    @Operation(summary = "📚 음악 공유 히스토리 조회", description = "사용자가 받은 음악 공유 히스토리를 조회합니다")
    public ResponseEntity<?> getMusicSharingHistory(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        try {
            Long userId = getCurrentUserId(oAuth2User);
            Pageable pageable = PageRequest.of(page, Math.min(size, 100));
            Page<MusicSharingHistory> history = musicSharingHistoryService.getUserMusicHistory(userId, pageable);
            
            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "음악 공유 히스토리를 성공적으로 조회했습니다";
                public final Object data = history;
            });
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "음악 공유 히스토리 조회 실패";
                public final String error = e.getMessage();
            });
        }
    }
    
    @GetMapping("/liked")
    @Operation(summary = "❤️ 좋아요한 음악 조회", description = "사용자가 좋아요 표시한 음악들을 조회합니다")
    public ResponseEntity<?> getLikedMusic(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Long userId = getCurrentUserId(oAuth2User);
            Pageable pageable = PageRequest.of(page, Math.min(size, 100));
            Page<MusicSharingHistory> likedMusic = musicSharingHistoryService.getUserLikedMusic(userId, pageable);
            
            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "좋아요한 음악을 성공적으로 조회했습니다";
                public final Object data = likedMusic;
            });
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "좋아요한 음악 조회 실패";
                public final String error = e.getMessage();
            });
        }
    }
    
    @GetMapping("/search")
    @Operation(summary = "🔍 음악 검색", description = "곡명이나 아티스트명으로 음악 히스토리를 검색합니다")
    public ResponseEntity<?> searchMusicHistory(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Long userId = getCurrentUserId(oAuth2User);
            Pageable pageable = PageRequest.of(page, Math.min(size, 100));
            Page<MusicSharingHistory> searchResults = musicSharingHistoryService.searchMusicHistory(userId, keyword, pageable);
            
            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "음악 검색을 성공적으로 완료했습니다";
                public final String searchKeyword = keyword;
                public final Object data = searchResults;
            });
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "음악 검색 실패";
                public final String error = e.getMessage();
            });
        }
    }
    
    @PostMapping("/{historyId}/like")
    @Operation(summary = "❤️ 음악 좋아요/취소", description = "음악에 좋아요를 표시하거나 취소합니다")
    public ResponseEntity<?> toggleLikeMusic(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @Parameter(description = "음악 히스토리 ID") @PathVariable Long historyId) {
        
        try {
            Long userId = getCurrentUserId(oAuth2User);
            MusicSharingHistory updatedHistory = musicSharingHistoryService.toggleLikeMusic(historyId, userId);
            
            String action = updatedHistory.getIsLiked() ? "좋아요" : "좋아요 취소";
            
            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = action + "를 성공적으로 처리했습니다";
                public final Object data = updatedHistory;
            });
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "좋아요 처리 실패";
                public final String error = e.getMessage();
            });
        }
    }
    
    @PostMapping("/{historyId}/note")
    @Operation(summary = "📝 메모 추가/수정", description = "음악에 개인 메모를 추가하거나 수정합니다")
    public ResponseEntity<?> addNoteToMusic(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @Parameter(description = "음악 히스토리 ID") @PathVariable Long historyId,
            @RequestBody Map<String, String> request) {
        
        try {
            Long userId = getCurrentUserId(oAuth2User);
            String notes = request.get("notes");
            
            MusicSharingHistory updatedHistory = musicSharingHistoryService.addNoteToMusic(historyId, userId, notes);
            
            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "메모를 성공적으로 저장했습니다";
                public final Object data = updatedHistory;
            });
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "메모 저장 실패";
                public final String error = e.getMessage();
            });
        }
    }
    
    @GetMapping("/stats")
    @Operation(summary = "📊 음악 공유 통계", description = "사용자의 음악 공유 통계를 조회합니다")
    public ResponseEntity<?> getMusicSharingStats(@AuthenticationPrincipal OAuth2User oAuth2User) {
        try {
            Long userId = getCurrentUserId(oAuth2User);
            Object stats = musicSharingHistoryService.getMusicSharingStats(userId);
            
            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "음악 공유 통계를 성공적으로 조회했습니다";
                public final Object data = stats;
            });
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "음악 공유 통계 조회 실패";
                public final String error = e.getMessage();
            });
        }
    }
    
    @GetMapping("/period")
    @Operation(summary = "📅 기간별 음악 히스토리", description = "특정 기간의 음악 공유 히스토리를 조회합니다")
    public ResponseEntity<?> getMusicHistoryByPeriod(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @Parameter(description = "시작 날짜 (yyyy-MM-dd)") @RequestParam String startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd)") @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Long userId = getCurrentUserId(oAuth2User);
            
            LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
            LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");
            
            Pageable pageable = PageRequest.of(page, Math.min(size, 100));
            Page<MusicSharingHistory> history = musicSharingHistoryService.getMusicHistoryByPeriod(
                    userId, startDateTime, endDateTime, pageable);
            
            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "기간별 음악 히스토리를 성공적으로 조회했습니다";
                public final String period = startDate + " ~ " + endDate;
                public final Object data = history;
            });
        } catch (Exception e) {
            return ResponseEntity.ok(new Object() {
                public final boolean success = false;
                public final String message = "기간별 음악 히스토리 조회 실패";
                public final String error = e.getMessage();
            });
        }
    }
}