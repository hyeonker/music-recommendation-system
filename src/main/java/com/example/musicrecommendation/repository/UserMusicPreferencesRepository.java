package com.example.musicrecommendation.repository;

import com.example.musicrecommendation.domain.UserMusicPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 음악 취향 프로필 데이터 액세스
 */
@Repository
public interface UserMusicPreferencesRepository extends JpaRepository<UserMusicPreferences, Long> {
    
    /**
     * 사용자 ID로 취향 프로필 조회
     */
    Optional<UserMusicPreferences> findByUserId(Long userId);
    
    /**
     * 최근 업데이트가 필요한 프로필 조회
     */
    @Query("SELECT p FROM UserMusicPreferences p WHERE p.lastUpdated < :cutoffTime")
    List<UserMusicPreferences> findStaleProfiles(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 유사한 취향을 가진 사용자 찾기 (장르 기반)
     * PostgreSQL의 JSONB 연산자 활용
     */
    @Query(value = "SELECT p1.user_id, p2.user_id, " +
                   "1 - (p1.genre_preferences <-> p2.genre_preferences) as similarity " +
                   "FROM user_music_preferences p1, user_music_preferences p2 " +
                   "WHERE p1.user_id = :userId AND p1.user_id != p2.user_id " +
                   "AND p1.genre_preferences IS NOT NULL AND p2.genre_preferences IS NOT NULL " +
                   "ORDER BY similarity DESC LIMIT :limit", 
           nativeQuery = true)
    List<Object[]> findSimilarUsersByGenre(@Param("userId") Long userId, @Param("limit") int limit);
    
    /**
     * 특정 장르를 선호하는 사용자들 찾기
     */
    @Query(value = "SELECT user_id, " +
                   "CAST(genre_preferences->>:genre AS DECIMAL) as preference_score " +
                   "FROM user_music_preferences " +
                   "WHERE jsonb_exists(genre_preferences, :genre) " +
                   "AND CAST(genre_preferences->>:genre AS DECIMAL) > :minScore " +
                   "ORDER BY preference_score DESC LIMIT :limit",
           nativeQuery = true)
    List<Object[]> findUsersWithGenrePreference(@Param("genre") String genre, 
                                               @Param("minScore") double minScore, 
                                               @Param("limit") int limit);
}