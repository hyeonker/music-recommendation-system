package com.example.musicrecommendation.repository;

import com.example.musicrecommendation.domain.RecommendationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecommendationHistoryRepository extends JpaRepository<RecommendationHistory, Long> {
    
    /**
     * 사용자별 총 추천곡 수 조회
     */
    long countByUserId(Long userId);
    
    /**
     * 사용자별 최근 추천곡 조회 (중복 방지용)
     */
    @Query("SELECT rh FROM RecommendationHistory rh WHERE rh.userId = :userId AND rh.createdAt >= :since ORDER BY rh.createdAt DESC")
    List<RecommendationHistory> findRecentRecommendationsByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);
    
    /**
     * 특정 트랙이 이미 추천되었는지 확인
     */
    boolean existsByUserIdAndTrackIdAndCreatedAtAfter(Long userId, String trackId, LocalDateTime after);
    
    /**
     * 특정 트랙 제목+아티스트 조합이 이미 추천되었는지 확인
     */
    boolean existsByUserIdAndTrackTitleAndTrackArtistAndCreatedAtAfter(
        Long userId, String trackTitle, String trackArtist, LocalDateTime after);
    
    /**
     * 사용자별 특정 기간 내 추천곡 수 조회
     */
    @Query("SELECT COUNT(rh) FROM RecommendationHistory rh WHERE rh.userId = :userId AND rh.createdAt >= :since")
    long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("since") LocalDateTime since);
    
    /**
     * 사용자별 추천 타입별 통계
     */
    @Query("SELECT rh.recommendationType, COUNT(rh) FROM RecommendationHistory rh WHERE rh.userId = :userId GROUP BY rh.recommendationType")
    List<Object[]> getRecommendationTypeStats(@Param("userId") Long userId);
    
    /**
     * 사용자별 최근 N개 추천 히스토리 조회
     */
    @Query("SELECT rh FROM RecommendationHistory rh WHERE rh.userId = :userId ORDER BY rh.createdAt DESC")
    List<RecommendationHistory> findTopByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 세션별 추천곡 조회 (같은 시점에 생성된 추천들)
     */
    List<RecommendationHistory> findByUserIdAndSessionIdOrderByCreatedAtDesc(Long userId, String sessionId);
}