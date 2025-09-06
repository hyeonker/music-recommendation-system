package com.example.musicrecommendation.repository;

import com.example.musicrecommendation.domain.MusicItem;
import com.example.musicrecommendation.domain.MusicReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MusicReviewRepository extends JpaRepository<MusicReview, Long> {
    
    Optional<MusicReview> findByUserIdAndMusicItem(Long userId, MusicItem musicItem);
    
    List<MusicReview> findByUserId(Long userId);
    
    Page<MusicReview> findByMusicItemAndIsPublicTrue(MusicItem musicItem, Pageable pageable);
    
    @Query("SELECT r FROM MusicReview r LEFT JOIN FETCH r.musicItem WHERE r.isPublic = true ORDER BY r.createdAt DESC")
    Page<MusicReview> findByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT r FROM MusicReview r LEFT JOIN FETCH r.musicItem WHERE r.userId = :userId AND r.isPublic = true ORDER BY r.createdAt DESC")
    Page<MusicReview> findByUserIdAndIsPublicTrue(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT r FROM MusicReview r LEFT JOIN FETCH r.musicItem WHERE r.isPublic = true AND r.rating >= :minRating ORDER BY r.helpfulCount DESC, r.createdAt DESC")
    Page<MusicReview> findHighRatedReviews(@Param("minRating") Integer minRating, Pageable pageable);
    
    @Query("SELECT r FROM MusicReview r LEFT JOIN FETCH r.musicItem WHERE r.isPublic = true AND r.helpfulCount >= :minHelpful ORDER BY r.helpfulCount DESC")
    Page<MusicReview> findMostHelpfulReviews(@Param("minHelpful") Integer minHelpful, Pageable pageable);
    
    @Query("SELECT r FROM MusicReview r WHERE r.isPublic = true AND r.createdAt >= :since ORDER BY r.createdAt DESC")
    Page<MusicReview> findRecentReviews(@Param("since") OffsetDateTime since, Pageable pageable);
    
    @Query("SELECT AVG(r.rating) FROM MusicReview r WHERE r.musicItem = :musicItem AND r.isPublic = true")
    Optional<Double> findAverageRatingByMusicItem(@Param("musicItem") MusicItem musicItem);
    
    @Query("SELECT COUNT(r) FROM MusicReview r WHERE r.musicItem = :musicItem AND r.isPublic = true")
    long countByMusicItemAndIsPublicTrue(@Param("musicItem") MusicItem musicItem);
    
    @Query("SELECT COUNT(r) FROM MusicReview r WHERE r.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    @Query(value = "SELECT * FROM music_reviews r WHERE r.is_public = true AND " +
           "(:tag IS NULL OR :tag = ANY(r.tags)) AND " +
           "(:minRating IS NULL OR r.rating >= :minRating) AND " +
           "(:maxRating IS NULL OR r.rating <= :maxRating) " +
           "ORDER BY r.created_at DESC",
           nativeQuery = true)
    Page<MusicReview> findReviewsWithFilters(
        @Param("tag") String tag,
        @Param("minRating") Integer minRating,
        @Param("maxRating") Integer maxRating,
        Pageable pageable
    );
    
    @Query(value = "SELECT DISTINCT UNNEST(tags) as tag FROM music_reviews WHERE is_public = true ORDER BY tag", 
           nativeQuery = true)
    List<String> findAllTags();
    
    /**
     * 사용자의 높은 평점 리뷰 조회 (추천 시스템용)
     */
    @Query("SELECT r FROM MusicReview r LEFT JOIN FETCH r.musicItem WHERE r.userId = :userId AND r.rating >= :minRating AND r.isPublic = true ORDER BY r.rating DESC, r.createdAt DESC")
    List<MusicReview> findByUserIdAndRatingGreaterThanEqual(@Param("userId") Long userId, @Param("minRating") Integer minRating);
    
    /**
     * 추천 시스템용: 사용자가 작성한 리뷰 + 사용자가 도움이 됨으로 표시한 리뷰
     */
    @Query(value = "SELECT DISTINCT r.* FROM music_reviews r " +
           "WHERE ((r.user_id = :userId AND r.rating >= :minRating) " +
           "OR (r.id IN (SELECT rh.review_id FROM review_helpful rh WHERE rh.user_id = :userId) AND r.rating >= :minRating)) " +
           "AND r.is_public = true " +
           "ORDER BY r.rating DESC, r.created_at DESC", 
           nativeQuery = true)
    List<MusicReview> findReviewsForRecommendations(@Param("userId") Long userId, @Param("minRating") Integer minRating);
    
    /**
     * 유사한 평점 패턴을 가진 사용자들 찾기
     */
    @Query(value = "SELECT DISTINCT r.user_id FROM music_reviews r " +
           "WHERE r.user_id != :userId AND r.is_public = true " +
           "GROUP BY r.user_id " +
           "HAVING ABS(AVG(r.rating::numeric) - :avgRating) <= :tolerance " +
           "ORDER BY ABS(AVG(r.rating::numeric) - :avgRating) " +
           "LIMIT :limit", 
           nativeQuery = true)
    List<Long> findUsersWithSimilarAverageRating(@Param("userId") Long userId, 
                                                @Param("avgRating") Double avgRating,
                                                @Param("tolerance") Double tolerance,
                                                @Param("limit") Integer limit);
    
    /**
     * 특정 아이템에 대한 평점 분포 조회
     */
    @Query(value = "SELECT r.rating, COUNT(*) as count FROM music_reviews r " +
           "WHERE r.music_item_id = :musicItemId AND r.is_public = true " +
           "GROUP BY r.rating ORDER BY r.rating", 
           nativeQuery = true)
    List<Object[]> findRatingDistributionByMusicItemId(@Param("musicItemId") Long musicItemId);
}