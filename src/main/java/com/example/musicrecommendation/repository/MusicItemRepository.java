package com.example.musicrecommendation.repository;

import com.example.musicrecommendation.domain.MusicItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MusicItemRepository extends JpaRepository<MusicItem, Long> {
    
    Optional<MusicItem> findByExternalIdAndItemType(String externalId, MusicItem.MusicItemType itemType);
    
    List<MusicItem> findByItemTypeAndNameContainingIgnoreCase(MusicItem.MusicItemType itemType, String name);
    
    List<MusicItem> findByArtistNameContainingIgnoreCase(String artistName);
    
    Page<MusicItem> findByGenre(String genre, Pageable pageable);
    
    @Query("SELECT m FROM MusicItem m WHERE " +
           "(:itemType IS NULL OR m.itemType = :itemType) AND " +
           "(:genre IS NULL OR m.genre = :genre) AND " +
           "(:searchTerm IS NULL OR " +
           "LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(m.artistName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(m.albumName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<MusicItem> searchMusicItems(
        @Param("itemType") MusicItem.MusicItemType itemType,
        @Param("genre") String genre,
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );
    
    @Query("SELECT DISTINCT m.genre FROM MusicItem m WHERE m.genre IS NOT NULL ORDER BY m.genre")
    List<String> findAllGenres();
    
    @Query("SELECT COUNT(m) FROM MusicItem m WHERE m.itemType = :itemType")
    long countByItemType(@Param("itemType") MusicItem.MusicItemType itemType);
}