package com.example.musicrecommendation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 음악 메타데이터 저장소
 */
@Repository
public interface MusicMetadataRepository extends JpaRepository<MusicMetadata, Long> {

    /**
     * 곡 ID로 메타데이터 조회
     */
    Optional<MusicMetadata> findBySongId(Long songId);

    /**
     * Spotify ID로 메타데이터 조회
     */
    Optional<MusicMetadata> findBySpotifyId(String spotifyId);

    /**
     * 메타데이터가 있는 곡들만 조회
     */
    @Query("SELECT m FROM MusicMetadata m WHERE m.song IS NOT NULL")
    List<MusicMetadata> findAllWithSong();

    /**
     * 특정 장르의 곡들 조회
     */
    @Query("SELECT m FROM MusicMetadata m WHERE m.genres LIKE %:genre%")
    List<MusicMetadata> findByGenreContaining(@Param("genre") String genre);

    /**
     * 에너지 범위로 곡 조회
     */
    @Query("SELECT m FROM MusicMetadata m WHERE m.energy BETWEEN :minEnergy AND :maxEnergy")
    List<MusicMetadata> findByEnergyBetween(@Param("minEnergy") Float minEnergy, @Param("maxEnergy") Float maxEnergy);

    /**
     * 댄스 가능성 범위로 곡 조회
     */
    @Query("SELECT m FROM MusicMetadata m WHERE m.danceability BETWEEN :minDance AND :maxDance")
    List<MusicMetadata> findByDanceabilityBetween(@Param("minDance") Float minDance, @Param("maxDance") Float maxDance);

    /**
     * 템포 범위로 곡 조회
     */
    @Query("SELECT m FROM MusicMetadata m WHERE m.tempo BETWEEN :minTempo AND :maxTempo")
    List<MusicMetadata> findByTempoBetween(@Param("minTempo") Float minTempo, @Param("maxTempo") Float maxTempo);

    /**
     * 인기도 순으로 곡 조회
     */
    @Query("SELECT m FROM MusicMetadata m ORDER BY m.popularity DESC")
    List<MusicMetadata> findAllOrderByPopularityDesc();

    /**
     * 메타데이터가 없는 곡들 조회 (Spotify API 호출 대상)
     */
    @Query("SELECT s FROM Song s WHERE s.id NOT IN (SELECT m.song.id FROM MusicMetadata m WHERE m.song IS NOT NULL)")
    List<Song> findSongsWithoutMetadata();

    /**
     * 유사한 음악 특성을 가진 곡들 조회
     */
    @Query("SELECT m FROM MusicMetadata m WHERE " +
            "ABS(m.energy - :energy) < :threshold AND " +
            "ABS(m.danceability - :danceability) < :threshold AND " +
            "ABS(m.valence - :valence) < :threshold " +
            "ORDER BY " +
            "(ABS(m.energy - :energy) + ABS(m.danceability - :danceability) + ABS(m.valence - :valence)) ASC")
    List<MusicMetadata> findSimilarByFeatures(@Param("energy") Float energy,
                                              @Param("danceability") Float danceability,
                                              @Param("valence") Float valence,
                                              @Param("threshold") Float threshold);

    /**
     * 특정 사용자가 좋아한 곡들의 메타데이터 조회
     */
    @Query("SELECT m FROM MusicMetadata m " +
            "INNER JOIN UserSongLike l ON l.song.id = m.song.id " +
            "WHERE l.user.id = :userId")
    List<MusicMetadata> findLikedSongMetadataByUserId(@Param("userId") Long userId);

    /**
     * 메타데이터 존재 여부 확인
     */
    boolean existsBySongId(Long songId);

    /**
     * 곡 개수 조회 (메타데이터 있는 곡들)
     */
    @Query("SELECT COUNT(m) FROM MusicMetadata m WHERE m.song IS NOT NULL")
    long countSongsWithMetadata();
}