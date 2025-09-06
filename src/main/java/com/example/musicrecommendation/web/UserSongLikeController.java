package com.example.musicrecommendation.web;

import com.example.musicrecommendation.domain.AuthProvider;
import com.example.musicrecommendation.domain.User;
import com.example.musicrecommendation.domain.UserBehaviorEvent;
import com.example.musicrecommendation.domain.UserRepository;
import com.example.musicrecommendation.domain.UserSongLike;
import com.example.musicrecommendation.domain.UserSongLikeRepository;
import com.example.musicrecommendation.service.UserBehaviorTrackingService;
import com.example.musicrecommendation.service.UserService;
import com.example.musicrecommendation.service.UserSongLikeService;
import com.example.musicrecommendation.web.dto.LikeResponse;
import com.example.musicrecommendation.web.dto.LikeToggleResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 음악 좋아요 관련 REST API를 제공하는 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/likes")
@Tag(name = "좋아요 API", description = "사용자 음악 좋아요 관리 API")
public class UserSongLikeController {

    private final UserSongLikeService userSongLikeService;
    private final UserRepository userRepository;
    private final UserBehaviorTrackingService behaviorTrackingService;

    public UserSongLikeController(UserSongLikeService userSongLikeService,
                                  UserRepository userRepository,
                                  UserBehaviorTrackingService behaviorTrackingService) {
        this.userSongLikeService = userSongLikeService;
        this.userRepository = userRepository;
        this.behaviorTrackingService = behaviorTrackingService;
    }

    /**
     * 좋아요 토글 (좋아요 추가/취소)
     */
    @PostMapping("/toggle")
    @Operation(summary = "좋아요 토글",
            description = "곡에 좋아요를 누르거나 취소합니다. 이미 좋아요를 눌렀다면 취소, 안 눌렀다면 추가됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토글 성공"),
            @ApiResponse(responseCode = "404", description = "사용자 또는 곡을 찾을 수 없음")
    })
    public ResponseEntity<LikeToggleResponse> toggleLike(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @RequestParam Long userId,
            @Parameter(description = "곡 ID", example = "1", required = true)
            @RequestParam Long songId) {

        try {
            boolean liked = userSongLikeService.toggleLike(userId, songId);
            long totalLikes = userSongLikeService.getSongLikeCount(songId);

            LikeToggleResponse response = new LikeToggleResponse(userId, songId, liked, totalLikes);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 사용자가 좋아요한 곡 목록 조회
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "사용자 좋아요 목록",
            description = "특정 사용자가 좋아요한 곡들을 최신순으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<Page<LikeResponse>> getUserLikedSongs(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "100")
            @RequestParam(defaultValue = "100") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<UserSongLike> likes = userSongLikeService.getUserLikedSongs(userId, pageable);
            Page<LikeResponse> responses = likes.map(LikeResponse::from);

            return ResponseEntity.ok(responses);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 특정 곡을 좋아요한 사용자 목록 조회
     */
    @GetMapping("/song/{songId}")
    @Operation(summary = "곡 좋아요 사용자 목록",
            description = "특정 곡을 좋아요한 사용자들을 최신순으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "곡을 찾을 수 없음")
    })
    public ResponseEntity<List<LikeResponse>> getSongLikedUsers(
            @Parameter(description = "곡 ID", example = "1")
            @PathVariable Long songId) {

        try {
            List<UserSongLike> likes = userSongLikeService.getSongLikedUsers(songId);
            List<LikeResponse> responses = likes.stream()
                    .map(LikeResponse::from)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 곡의 좋아요 수 조회
     */
    @GetMapping("/song/{songId}/count")
    @Operation(summary = "곡 좋아요 수",
            description = "특정 곡의 총 좋아요 수를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<Long> getSongLikeCount(
            @Parameter(description = "곡 ID", example = "1")
            @PathVariable Long songId) {

        long count = userSongLikeService.getSongLikeCount(songId);
        return ResponseEntity.ok(count);
    }

    /**
     * 사용자의 좋아요 수 조회
     */
    @GetMapping("/user/{userId}/count")
    @Operation(summary = "사용자 좋아요 수",
            description = "특정 사용자가 누른 총 좋아요 수를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<Long> getUserLikeCount(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId) {

        long count = userSongLikeService.getUserLikeCount(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * 사용자가 특정 곡에 좋아요를 눌렀는지 확인
     */
    @GetMapping("/check")
    @Operation(summary = "좋아요 상태 확인",
            description = "사용자가 특정 곡에 좋아요를 눌렀는지 확인합니다.")
    @ApiResponse(responseCode = "200", description = "확인 완료")
    public ResponseEntity<Boolean> checkLikeStatus(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @RequestParam Long userId,
            @Parameter(description = "곡 ID", example = "1", required = true)
            @RequestParam Long songId) {

        boolean isLiked = userSongLikeService.isLikedByUser(userId, songId);
        return ResponseEntity.ok(isLiked);
    }

    /**
     * 가장 많이 좋아요 받은 곡들 TOP N
     */
    @GetMapping("/top-songs")
    @Operation(summary = "TOP 좋아요 곡",
            description = "가장 많이 좋아요를 받은 곡들을 순위별로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<UserSongLikeRepository.SongLikeCount>> getTopLikedSongs(
            @Parameter(description = "조회할 개수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        List<UserSongLikeRepository.SongLikeCount> topSongs =
                userSongLikeService.getTopLikedSongs(limit);

        return ResponseEntity.ok(topSongs);
    }

    /**
     * 전체 좋아요 수 통계
     */
    @GetMapping("/total-count")
    @Operation(summary = "전체 좋아요 수",
            description = "시스템 전체의 총 좋아요 수를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<Long> getTotalLikeCount() {

        long totalCount = userSongLikeService.getTotalLikeCount();
        return ResponseEntity.ok(totalCount);
    }

    /**
     * 외부 음악 서비스(Spotify 등)에서 가져온 곡 좋아요 추가
     */
    @PostMapping("/song")
    @Operation(summary = "외부 음악 좋아요",
            description = "Spotify 등 외부 서비스의 음악에 좋아요를 추가합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "좋아요 추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<LikeToggleResponse> likeExternalSong(
            @RequestBody ExternalSongLikeRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        try {
            log.debug("외부 음악 좋아요 요청 받음: songId={}, title={}", request.getSongId(), request.getTitle());
            
            if (oauth2User == null) {
                log.warn("인증되지 않은 사용자의 좋아요 요청");
                return ResponseEntity.status(401).build(); // Unauthorized
            }

            Long userId = getCurrentUserId(oauth2User);
            log.debug("인증된 사용자 ID: {}", userId);
            
            // Spotify ID를 해시코드로 변환하여 Long ID 생성
            Long songIdAsLong = (long) Math.abs(request.getSongId().hashCode());
            
            // 외부 음악 좋아요 토글 (Song이 없으면 자동 생성)
            UserSongLikeService.ToggleResult result = userSongLikeService.toggleExternalSong(
                userId, 
                songIdAsLong, 
                request.getTitle(), 
                request.getArtist(), 
                request.getImageUrl()
            );
            
            // 실제 생성된 Song ID로 좋아요 수 조회
            long totalLikes = userSongLikeService.getSongLikeCount(result.getActualSongId());

            // 행동 추적 (좋아요인 경우만)
            if (result.isLiked()) {
                java.util.Map<String, Object> metadata = new java.util.HashMap<>();
                metadata.put("title", request.getTitle());
                metadata.put("artist", request.getArtist());
                if (request.getAlbum() != null) {
                    metadata.put("album", request.getAlbum());
                }
                
                behaviorTrackingService.trackLikeEvent(
                    userId,
                    request.getSongId(),
                    UserBehaviorEvent.ItemType.SONG,
                    metadata,
                    true // isLike
                );
            }

            LikeToggleResponse response = new LikeToggleResponse(userId, result.getActualSongId(), result.isLiked(), totalLikes);
            log.debug("좋아요 처리 완료: userId={}, actualSongId={}, liked={}", userId, result.getActualSongId(), result.isLiked());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("외부 음악 좋아요 처리 중 오류 발생", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 외부 음악 서비스 곡 좋아요 취소
     */
    @DeleteMapping("/song/{songId}")
    @Operation(summary = "외부 음악 좋아요 취소",
            description = "Spotify 등 외부 서비스의 음악 좋아요를 취소합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "좋아요 취소 성공"),
            @ApiResponse(responseCode = "404", description = "좋아요를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<LikeToggleResponse> unlikeExternalSong(
            @PathVariable String songId,
            @RequestParam String title,
            @RequestParam String artist,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        try {
            log.debug("외부 음악 좋아요 취소 요청 받음: songId={}, title={}, artist={}", songId, title, artist);
            
            if (oauth2User == null) {
                log.warn("인증되지 않은 사용자의 좋아요 취소 요청");
                return ResponseEntity.status(401).build(); // Unauthorized
            }

            Long userId = getCurrentUserId(oauth2User);
            log.debug("인증된 사용자 ID: {}", userId);
            
            // title과 artist를 사용하여 곡 찾기 (좋아요할 때와 동일한 방식)
            boolean unliked = userSongLikeService.unlikeExternalSongByTitleArtist(userId, title, artist);
            
            if (!unliked) {
                // 좋아요 상태가 아니었거나 곡을 찾을 수 없는 경우
                log.debug("좋아요 취소 실패: userId={}, title={}, artist={}", userId, title, artist);
                return ResponseEntity.notFound().build();
            }
            
            // 응답을 위해 해시 ID 사용 (일관성을 위해)
            Long hashSongId = (long) Math.abs(songId.hashCode());
            long totalLikes = 0; // 좋아요 취소 후이므로 정확한 개수를 다시 계산하지 않음

            LikeToggleResponse response = new LikeToggleResponse(userId, hashSongId, false, totalLikes);
            log.debug("좋아요 취소 완료: userId={}, hashSongId={}", userId, hashSongId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("좋아요 취소 중 잘못된 인수: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("외부 음악 좋아요 취소 처리 중 오류 발생", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 외부 음악 좋아요 요청 DTO
     */
    public static class ExternalSongLikeRequest {
        
        @JsonProperty("songId")
        private String songId;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("artist")
        private String artist;
        
        @JsonProperty("imageUrl")
        private String imageUrl;
        
        @JsonProperty("externalId")
        private String externalId;
        
        @JsonProperty("album")
        private String album;
        
        @JsonProperty("previewUrl")
        private String previewUrl;
        
        @JsonProperty("spotifyUrl")
        private String spotifyUrl;

        // 기본 생성자
        public ExternalSongLikeRequest() {}

        // Getters and Setters
        public String getSongId() { return songId; }
        public void setSongId(String songId) { this.songId = songId; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }
        
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        
        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        
        public String getAlbum() { return album; }
        public void setAlbum(String album) { this.album = album; }
        
        public String getPreviewUrl() { return previewUrl; }
        public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
        
        public String getSpotifyUrl() { return spotifyUrl; }
        public void setSpotifyUrl(String spotifyUrl) { this.spotifyUrl = spotifyUrl; }
    }

    /**
     * OAuth2User에서 사용자 ID 추출
     */
    private Long getCurrentUserId(OAuth2User oauth2User) {
        if (oauth2User == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        java.util.Map<String, Object> attrs = oauth2User.getAttributes();
        log.debug("OAuth2 사용자 속성: {}", attrs.keySet());
        
        String provider;
        String providerId;

        // Google OAuth2의 경우
        if (attrs.containsKey("email") && attrs.containsKey("sub")) {
            provider = "google";
            providerId = attrs.get("sub").toString();
        }
        // Kakao OAuth2의 경우
        else if (attrs.containsKey("id")) {
            provider = "kakao";
            providerId = attrs.get("id").toString();
        }
        // Naver OAuth2의 경우
        else if (attrs.containsKey("response")) {
            provider = "naver";
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> response = (java.util.Map<String, Object>) attrs.get("response");
            providerId = response.get("id").toString();
        }
        // 기타 제공자
        else {
            throw new IllegalArgumentException("지원하지 않는 OAuth2 제공자입니다.");
        }

        // 데이터베이스에서 사용자 조회
        AuthProvider authProvider = AuthProvider.valueOf(provider.toUpperCase());
        java.util.Optional<User> userOpt = userRepository.findByProviderAndProviderId(authProvider, providerId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        return userOpt.get().getId();
    }
}