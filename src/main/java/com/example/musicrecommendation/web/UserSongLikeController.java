package com.example.musicrecommendation.web;

import com.example.musicrecommendation.domain.UserSongLike;
import com.example.musicrecommendation.domain.UserSongLikeRepository;
import com.example.musicrecommendation.service.UserSongLikeService;
import com.example.musicrecommendation.web.dto.LikeResponse;
import com.example.musicrecommendation.web.dto.LikeToggleResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 음악 좋아요 관련 REST API를 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/api/likes")
@Tag(name = "좋아요 API", description = "사용자 음악 좋아요 관리 API")
public class UserSongLikeController {

    private final UserSongLikeService userSongLikeService;

    public UserSongLikeController(UserSongLikeService userSongLikeService) {
        this.userSongLikeService = userSongLikeService;
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
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {

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
            @RequestBody ExternalSongLikeRequest request) {

        try {
            // 현재는 기본 사용자 ID 1 사용 (나중에 인증에서 가져오도록 수정 가능)
            Long userId = 1L;
            
            boolean liked = userSongLikeService.likeExternalSong(
                    userId,
                    request.getSongId(),
                    request.getTitle(),
                    request.getArtist(),
                    request.getImageUrl()
            );
            
            // songId를 Long으로 파싱 (Spotify ID를 해시코드로 변환)
            Long songIdAsLong = (long) request.getSongId().hashCode();
            long totalLikes = userSongLikeService.getSongLikeCount(songIdAsLong);

            LikeToggleResponse response = new LikeToggleResponse(userId, songIdAsLong, liked, totalLikes);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
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
            @ApiResponse(responseCode = "404", description = "좋아요를 찾을 수 없음")
    })
    public ResponseEntity<LikeToggleResponse> unlikeExternalSong(
            @PathVariable String songId) {

        try {
            // 현재는 기본 사용자 ID 1 사용
            Long userId = 1L;
            
            boolean liked = userSongLikeService.unlikeExternalSong(userId, songId);
            
            // songId를 Long으로 파싱 (Spotify ID를 해시코드로 변환)
            Long songIdAsLong = (long) songId.hashCode();
            long totalLikes = userSongLikeService.getSongLikeCount(songIdAsLong);

            LikeToggleResponse response = new LikeToggleResponse(userId, songIdAsLong, liked, totalLikes);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
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
}