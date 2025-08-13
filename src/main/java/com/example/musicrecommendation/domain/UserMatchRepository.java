package com.example.musicrecommendation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 매칭 저장소
 */
@Repository
public interface UserMatchRepository extends JpaRepository<UserMatch, Long> {

    /**
     * 사용자의 모든 매칭 조회
     */
    @Query("SELECT um FROM UserMatch um WHERE um.user1Id = :userId OR um.user2Id = :userId ORDER BY um.similarityScore DESC")
    List<UserMatch> findMatchesByUserId(@Param("userId") Long userId);

    /**
     * 특정 매칭 상태의 매칭들 조회
     */
    @Query("SELECT um FROM UserMatch um WHERE (um.user1Id = :userId OR um.user2Id = :userId) AND um.matchStatus = :status")
    List<UserMatch> findMatchesByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 두 사용자 간 매칭 조회
     */
    @Query("SELECT um FROM UserMatch um WHERE (um.user1Id = :user1Id AND um.user2Id = :user2Id) OR (um.user1Id = :user2Id AND um.user2Id = :user1Id)")
    Optional<UserMatch> findMatchBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    /**
     * 높은 유사도 매칭들 조회
     */
    @Query("SELECT um FROM UserMatch um WHERE um.similarityScore >= :minScore ORDER BY um.similarityScore DESC")
    List<UserMatch> findHighSimilarityMatches(@Param("minScore") Double minScore);

    /**
     * 사용자의 상위 매칭들 조회
     */
    @Query("SELECT um FROM UserMatch um WHERE (um.user1Id = :userId OR um.user2Id = :userId) ORDER BY um.similarityScore DESC LIMIT :limit")
    List<UserMatch> findTopMatchesByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 매칭 통계 조회
     */
    @Query("SELECT COUNT(um) FROM UserMatch um WHERE um.matchStatus = :status")
    Long countByStatus(@Param("status") String status);
}