package com.example.musicrecommendation.repository;

import com.example.musicrecommendation.domain.UserBehaviorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 행동 이벤트 데이터 액세스
 */
@Repository
public interface UserBehaviorEventRepository extends JpaRepository<UserBehaviorEvent, Long> {
    
    /**
     * 사용자의 최근 행동 이벤트 조회
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.userId = :userId " +
           "AND e.createdAt >= :since ORDER BY e.createdAt DESC")
    List<UserBehaviorEvent> findRecentUserEvents(@Param("userId") Long userId, 
                                                @Param("since") LocalDateTime since);
    
    /**
     * 특정 아이템에 대한 사용자 행동 조회
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.userId = :userId " +
           "AND e.itemId = :itemId AND e.itemType = :itemType " +
           "ORDER BY e.createdAt DESC")
    List<UserBehaviorEvent> findUserItemEvents(@Param("userId") Long userId,
                                              @Param("itemId") String itemId,
                                              @Param("itemType") UserBehaviorEvent.ItemType itemType);
    
    /**
     * 긍정적 행동 이벤트 조회 (좋아요, 긴 재생 등)
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.userId = :userId " +
           "AND (e.eventType = 'LIKE' OR e.eventType = 'PLAYLIST_ADD' OR e.eventType = 'SHARE' " +
           "OR (e.eventType = 'PLAY' AND e.durationSeconds > 30)) " +
           "AND e.createdAt >= :since ORDER BY e.createdAt DESC")
    List<UserBehaviorEvent> findPositiveUserEvents(@Param("userId") Long userId,
                                                  @Param("since") LocalDateTime since);
    
    /**
     * 특정 아이템에 대한 모든 사용자의 행동 (협업 필터링용)
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.itemId = :itemId " +
           "AND e.itemType = :itemType AND e.createdAt >= :since")
    List<UserBehaviorEvent> findItemEvents(@Param("itemId") String itemId,
                                         @Param("itemType") UserBehaviorEvent.ItemType itemType,
                                         @Param("since") LocalDateTime since);
    
    /**
     * 사용자별 장르별 행동 통계
     */
    @Query("SELECT " +
           "JSON_EXTRACT(e.itemMetadata, '$.genre') as genre, " +
           "COUNT(*) as eventCount, " +
           "AVG(e.durationSeconds) as avgDuration, " +
           "AVG(CASE " +
           "  WHEN e.eventType = 'LIKE' THEN 1.0 " +
           "  WHEN e.eventType = 'PLAYLIST_ADD' THEN 0.9 " +
           "  WHEN e.eventType = 'SHARE' THEN 0.85 " +
           "  WHEN e.eventType = 'PLAY' AND e.durationSeconds > 120 THEN 0.8 " +
           "  WHEN e.eventType = 'PLAY' AND e.durationSeconds > 30 THEN 0.5 " +
           "  WHEN e.eventType = 'SKIP' THEN 0.2 " +
           "  WHEN e.eventType = 'DISLIKE' THEN 0.0 " +
           "  ELSE 0.4 " +
           "END) as avgScore " +
           "FROM UserBehaviorEvent e " +
           "WHERE e.userId = :userId AND e.createdAt >= :since " +
           "AND JSON_EXTRACT(e.itemMetadata, '$.genre') IS NOT NULL " +
           "GROUP BY JSON_EXTRACT(e.itemMetadata, '$.genre')")
    List<Object[]> getUserGenreStatistics(@Param("userId") Long userId,
                                         @Param("since") LocalDateTime since);
    
    /**
     * 오래된 이벤트 정리 (성능 최적화)
     */
    @Modifying
    @Query("DELETE FROM UserBehaviorEvent e WHERE e.createdAt < :cutoffDate")
    int deleteOldEvents(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * 세션별 사용자 행동 조회
     */
    List<UserBehaviorEvent> findBySessionIdOrderByCreatedAt(String sessionId);
    
    /**
     * 사용자의 최근 검색 이벤트
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.userId = :userId " +
           "AND e.eventType = 'SEARCH' AND e.createdAt >= :since " +
           "ORDER BY e.createdAt DESC")
    List<UserBehaviorEvent> findRecentSearchEvents(@Param("userId") Long userId,
                                                  @Param("since") LocalDateTime since);
}