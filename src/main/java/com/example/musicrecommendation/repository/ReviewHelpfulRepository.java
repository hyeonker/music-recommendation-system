package com.example.musicrecommendation.repository;

import com.example.musicrecommendation.domain.ReviewHelpful;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewHelpfulRepository extends JpaRepository<ReviewHelpful, Long> {
    
    Optional<ReviewHelpful> findByReviewIdAndUserId(Long reviewId, Long userId);
    
    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);
    
    long countByReviewId(Long reviewId);
    
    @Query("SELECT rh.reviewId FROM ReviewHelpful rh WHERE rh.userId = :userId AND rh.reviewId IN :reviewIds")
    List<Long> findHelpfulReviewIdsByUserAndReviewIds(@Param("userId") Long userId, @Param("reviewIds") List<Long> reviewIds);
}