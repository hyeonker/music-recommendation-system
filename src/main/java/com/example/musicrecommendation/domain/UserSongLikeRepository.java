package com.example.musicrecommendation.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 음악 좋아요 데이터 접근을 위한 Repository 인터페이스
 */
@Repository
public interface UserSongLikeRepository extends JpaRepository<UserSongLike, Long> {

    /**
     * 특정 사용자와 곡의 좋아요 찾기
     * 중복 좋아요 방지 및 좋아요 취소용
     *
     * @param userId 사용자 ID
     * @param songId 곡 ID
     * @return 좋아요 Optional (없으면 empty)
     */
    Optional<UserSongLike> findByUserIdAndSongId(Long userId, Long songId);

    /**
     * 특정 사용자가 좋아요한 모든 곡 조회 (페이징)
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 좋아요한 곡 목록 (페이징)
     */
    Page<UserSongLike> findByUserIdOrderByLikedAtDesc(Long userId, Pageable pageable);

    /**
     * 특정 사용자가 좋아요한 모든 곡 조회 (페이징) - EAGER FETCH
     * lazy loading 문제 해결을 위해 JOIN FETCH 사용
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 좋아요한 곡 목록 (페이징)
     */
    @Query("SELECT usl FROM UserSongLike usl " +
           "JOIN FETCH usl.user " +
           "JOIN FETCH usl.song " +
           "WHERE usl.user.id = :userId " +
           "ORDER BY usl.likedAt DESC")
    Page<UserSongLike> findByUserIdWithSongAndUserOrderByLikedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 곡을 좋아요한 모든 사용자 조회
     *
     * @param songId 곡 ID
     * @return 좋아요한 사용자 목록
     */
    List<UserSongLike> findBySongIdOrderByLikedAtDesc(Long songId);

    /**
     * 특정 곡의 좋아요 수 조회
     *
     * @param songId 곡 ID
     * @return 좋아요 수
     */
    long countBySongId(Long songId);

    /**
     * 특정 사용자가 좋아요한 곡 수 조회
     *
     * @param userId 사용자 ID
     * @return 좋아요한 곡 수
     */
    long countByUserId(Long userId);

    /**
     * 특정 사용자와 곡의 좋아요 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @param songId 곡 ID
     * @return 존재하면 true
     */
    boolean existsByUserIdAndSongId(Long userId, Long songId);

    /**
     * 가장 많이 좋아요 받은 곡들 TOP N 조회
     *
     * @param limit 조회할 개수
     * @return 곡 ID와 좋아요 수 목록
     */
    @Query("SELECT l.song.id as songId, COUNT(l) as likeCount " +
            "FROM UserSongLike l " +
            "GROUP BY l.song.id " +
            "ORDER BY COUNT(l) DESC " +
            "LIMIT :limit")
    List<SongLikeCount> findTopLikedSongs(@Param("limit") int limit);

    /**
     * 곡 ID와 좋아요 수를 담는 인터페이스 (Projection용)
     */
    interface SongLikeCount {
        Long getSongId();
        Long getLikeCount();
    }

    /**
     * 특정 사용자가 좋아요한 곡 ID 목록 조회 (간단한 형태)
     * 추천 알고리즘에서 사용
     *
     * @param userId 사용자 ID
     * @return 좋아요한 곡 ID 목록
     */
    @Query("SELECT l.song.id FROM UserSongLike l WHERE l.user.id = :userId")
    List<Long> findLikedSongIdsByUserId(@Param("userId") Long userId);

    // === 🔥 HOT 통계를 위한 간소화된 쿼리들 ===

    /**
     * 특정 기간 동안 가장 많이 좋아요 받은 곡들 조회 (LIMIT 대신 Pageable 사용)
     */
    @Query("SELECT l.song.id as songId, COUNT(l) as likeCount " +
            "FROM UserSongLike l " +
            "WHERE l.likedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY l.song.id " +
            "ORDER BY COUNT(l) DESC")
    List<SongLikeCount> findTopLikedSongsByPeriod(@Param("startDate") java.time.LocalDateTime startDate,
                                                  @Param("endDate") java.time.LocalDateTime endDate,
                                                  org.springframework.data.domain.Pageable pageable);

    /**
     * 특정 기간 동안 가장 활발한 사용자들 조회
     */
    @Query("SELECT l.user.id as userId, COUNT(l) as likeCount " +
            "FROM UserSongLike l " +
            "WHERE l.likedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY l.user.id " +
            "ORDER BY COUNT(l) DESC")
    List<UserLikeCount> findMostActiveUsersByPeriod(@Param("startDate") java.time.LocalDateTime startDate,
                                                    @Param("endDate") java.time.LocalDateTime endDate,
                                                    org.springframework.data.domain.Pageable pageable);

    /**
     * 특정 기간 동안 아티스트별 좋아요 수 집계
     */
    @Query("SELECT l.song.artist as artistName, COUNT(l) as likeCount, COUNT(DISTINCT l.song.id) as songCount " +
            "FROM UserSongLike l " +
            "WHERE l.likedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY l.song.artist " +
            "ORDER BY COUNT(l) DESC")
    List<ArtistLikeCount> findTopArtistsByPeriod(@Param("startDate") java.time.LocalDateTime startDate,
                                                 @Param("endDate") java.time.LocalDateTime endDate,
                                                 org.springframework.data.domain.Pageable pageable);

    /**
     * 현재 기간의 곡별 좋아요 수 (급상승 계산용 - 단순화)
     */
    @Query("SELECT l.song.id as songId, COUNT(l) as likeCount " +
            "FROM UserSongLike l " +
            "WHERE l.likedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY l.song.id")
    List<SongLikeCount> findSongLikesByPeriod(@Param("startDate") java.time.LocalDateTime startDate,
                                              @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * 특정 기간 동안의 좋아요 수 카운트 (오늘 좋아요 등)
     */
    long countByLikedAtBetween(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    /**
     * 특정 아티스트의 곡들을 좋아요한 고유 사용자 수 (팬 수)
     *
     * @param artistName 아티스트명
     * @return 팬 수
     */
    @Query("SELECT COUNT(DISTINCT l.user.id) " +
            "FROM UserSongLike l " +
            "WHERE l.song.artist = :artistName")
    long countFansByArtist(@Param("artistName") String artistName);

    // === 📊 Projection 인터페이스들 ===

    /**
     * 사용자 ID와 좋아요 수를 담는 인터페이스
     */
    interface UserLikeCount {
        Long getUserId();
        Long getLikeCount();
    }

    /**
     * 아티스트와 좋아요 통계를 담는 인터페이스
     */
    interface ArtistLikeCount {
        String getArtistName();
        Long getLikeCount();
        Long getSongCount();
    }
}