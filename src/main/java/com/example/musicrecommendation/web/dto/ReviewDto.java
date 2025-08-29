package com.example.musicrecommendation.web.dto;

import com.example.musicrecommendation.domain.MusicItem;
import com.example.musicrecommendation.domain.MusicReview;
import lombok.*;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;

public class ReviewDto {
    
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        
        @NotBlank(message = "외부 ID는 필수입니다")
        private String externalId;
        
        @NotNull(message = "아이템 타입은 필수입니다")
        private MusicItem.MusicItemType itemType;
        
        // 음악 정보 필드들
        @NotBlank(message = "음악 이름은 필수입니다")
        private String musicName;
        
        @NotBlank(message = "아티스트 이름은 필수입니다")
        private String artistName;
        
        private String albumName;
        private String imageUrl;
        
        @NotNull(message = "평점은 필수입니다")
        @Min(value = 1, message = "평점은 최소 1점입니다")
        @Max(value = 5, message = "평점은 최대 5점입니다")
        private Integer rating;
        
        @Size(max = 2000, message = "리뷰 텍스트는 2000자를 초과할 수 없습니다")
        private String reviewText;
        
        @Size(max = 10, message = "태그는 최대 10개까지 가능합니다")
        private List<@Size(max = 50, message = "태그는 50자를 초과할 수 없습니다") String> tags;
        
        @Builder.Default
        private Boolean isPublic = true;
        @Builder.Default
        private Boolean isSpoiler = false;
    }
    
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UpdateRequest {
        
        @Min(value = 1, message = "평점은 최소 1점입니다")
        @Max(value = 5, message = "평점은 최대 5점입니다")
        private Integer rating;
        
        @Size(max = 2000, message = "리뷰 텍스트는 2000자를 초과할 수 없습니다")
        private String reviewText;
        
        @Size(max = 10, message = "태그는 최대 10개까지 가능합니다")
        private List<@Size(max = 50, message = "태그는 50자를 초과할 수 없습니다") String> tags;
        
        private Boolean isPublic;
        private Boolean isSpoiler;
    }
    
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private Long userId;
        private String userNickname; // 사용자 서비스에서 가져와 설정
        private MusicItemDto musicItem;
        private Integer rating;
        private String ratingEmoji;
        private String reviewText;
        private List<String> tags;
        private Boolean isSpoiler;
        private Boolean isPublic;
        private Integer helpfulCount;
        private Integer reportCount;
        private Boolean isHelpful;
        private Boolean isReported;
        private Boolean isShortReview;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        
        public static Response from(MusicReview review) {
            return Response.builder()
                    .id(review.getId())
                    .userId(review.getUserId())
                    .userNickname(null) // 별도로 설정 필요
                    .musicItem(MusicItemDto.from(review.getMusicItem()))
                    .rating(review.getRating())
                    .ratingEmoji(review.getRatingEmoji())
                    .reviewText(review.getReviewText())
                    .tags(review.getTags())
                    .isSpoiler(review.getIsSpoiler())
                    .isPublic(review.getIsPublic())
                    .helpfulCount(review.getHelpfulCount())
                    .reportCount(review.getReportCount())
                    .isHelpful(review.isHelpful())
                    .isReported(review.isReported())
                    .isShortReview(review.isShortReview())
                    .createdAt(review.getCreatedAt())
                    .updatedAt(review.getUpdatedAt())
                    .build();
        }
        
        public static Response fromWithUserNickname(MusicReview review, String userNickname) {
            return Response.builder()
                    .id(review.getId())
                    .userId(review.getUserId())
                    .userNickname(userNickname)
                    .musicItem(MusicItemDto.from(review.getMusicItem()))
                    .rating(review.getRating())
                    .ratingEmoji(review.getRatingEmoji())
                    .reviewText(review.getReviewText())
                    .tags(review.getTags())
                    .isSpoiler(review.getIsSpoiler())
                    .isPublic(review.getIsPublic())
                    .helpfulCount(review.getHelpfulCount())
                    .reportCount(review.getReportCount())
                    .isHelpful(review.isHelpful())
                    .isReported(review.isReported())
                    .isShortReview(review.isShortReview())
                    .createdAt(review.getCreatedAt())
                    .updatedAt(review.getUpdatedAt())
                    .build();
        }
    }
    
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MusicItemDto {
        private Long id;
        private String externalId;
        private MusicItem.MusicItemType itemType;
        private String itemTypeDisplay;
        private String name;
        private String artistName;
        private String albumName;
        private String genre;
        private String imageUrl;
        private String externalUrl;
        
        public static MusicItemDto from(MusicItem item) {
            return MusicItemDto.builder()
                    .id(item.getId())
                    .externalId(item.getExternalId())
                    .itemType(item.getItemType())
                    .itemTypeDisplay(item.getItemType().getDisplayName())
                    .name(item.getName())
                    .artistName(item.getArtistName())
                    .albumName(item.getAlbumName())
                    .genre(item.getGenre())
                    .imageUrl(item.getImageUrl())
                    .externalUrl(item.getExternalUrl())
                    .build();
        }
    }
    
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SearchRequest {
        private String tag;
        
        @Min(value = 1, message = "최소 평점은 1점입니다")
        @Max(value = 5, message = "최대 평점은 5점입니다")
        private Integer minRating;
        
        @Min(value = 1, message = "최소 평점은 1점입니다")
        @Max(value = 5, message = "최대 평점은 5점입니다")
        private Integer maxRating;
        
        @Min(value = 0, message = "페이지는 0 이상이어야 합니다")
        @Builder.Default
        private Integer page = 0;
        
        @Min(value = 1, message = "사이즈는 1 이상이어야 합니다")
        @Max(value = 100, message = "사이즈는 100 이하여야 합니다")
        @Builder.Default
        private Integer size = 20;
        
        @Builder.Default
        private String sortBy = "createdAt";
        @Builder.Default
        private String sortDirection = "desc";
    }
}