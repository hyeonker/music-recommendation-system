package com.example.musicrecommendation.repository;

import com.example.musicrecommendation.domain.MusicSharingHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MusicSharingHistoryRepository extends JpaRepository<MusicSharingHistory, Long> {
    
    /**
     * 특정 사용자가 받은 음악 공유 히스토리 조회 (최신순)
     */
    Page<MusicSharingHistory> findByUserIdOrderBySharedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 특정 사용자가 좋아요 표시한 음악들만 조회
     */
    Page<MusicSharingHistory> findByUserIdAndIsLikedTrueOrderByLikedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 특정 사용자가 공유한 음악들 조회
     */
    Page<MusicSharingHistory> findBySharedByUserIdOrderBySharedAtDesc(Long sharedByUserId, Pageable pageable);
    
    /**
     * 두 사용자 간의 음악 공유 히스토리 조회
     */
    @Query("SELECT msh FROM MusicSharingHistory msh WHERE " +
           "(msh.userId = :userId1 AND msh.sharedByUserId = :userId2) OR " +
           "(msh.userId = :userId2 AND msh.sharedByUserId = :userId1) " +
           "ORDER BY msh.sharedAt DESC")
    Page<MusicSharingHistory> findMusicSharingBetweenUsers(
            @Param("userId1") Long userId1, 
            @Param("userId2") Long userId2, 
            Pageable pageable);
    
    /**
     * 특정 사용자의 음악 공유 통계 조회
     */
    @Query("SELECT COUNT(msh) FROM MusicSharingHistory msh WHERE msh.userId = :userId")
    Long countReceivedMusic(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(msh) FROM MusicSharingHistory msh WHERE msh.sharedByUserId = :userId")
    Long countSharedMusic(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(msh) FROM MusicSharingHistory msh WHERE msh.userId = :userId AND msh.isLiked = true")
    Long countLikedMusic(@Param("userId") Long userId);
    
    /**
     * 곡명이나 아티스트명으로 검색
     */
    @Query("SELECT msh FROM MusicSharingHistory msh WHERE msh.userId = :userId AND " +
           "(LOWER(msh.trackName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(msh.artistName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY msh.sharedAt DESC")
    Page<MusicSharingHistory> searchMusicHistory(@Param("userId") Long userId, 
                                                 @Param("keyword") String keyword, 
                                                 Pageable pageable);
    
    /**
     * 특정 기간 내의 음악 공유 히스토리
     */
    Page<MusicSharingHistory> findByUserIdAndSharedAtBetweenOrderBySharedAtDesc(
            Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * 중복 공유 확인 (같은 곡을 같은 사용자가 이미 공유했는지)
     */
    Optional<MusicSharingHistory> findByUserIdAndSharedByUserIdAndTrackNameAndArtistName(
            Long userId, Long sharedByUserId, String trackName, String artistName);
    
    /**
     * 가장 많이 공유받은 곡들 (Top N)
     */
    @Query("SELECT msh.trackName, msh.artistName, COUNT(msh) as shareCount " +
           "FROM MusicSharingHistory msh WHERE msh.userId = :userId " +
           "GROUP BY msh.trackName, msh.artistName " +
           "ORDER BY shareCount DESC")
    List<Object[]> findMostSharedTracks(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 가장 많이 음악을 공유해준 사용자들
     */
    @Query("SELECT msh.sharedByUserId, msh.sharedByName, COUNT(msh) as shareCount " +
           "FROM MusicSharingHistory msh WHERE msh.userId = :userId " +
           "GROUP BY msh.sharedByUserId, msh.sharedByName " +
           "ORDER BY shareCount DESC")
    List<Object[]> findTopSharers(@Param("userId") Long userId, Pageable pageable);
}