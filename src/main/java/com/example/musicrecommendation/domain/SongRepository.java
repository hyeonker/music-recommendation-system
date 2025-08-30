package com.example.musicrecommendation.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
// @Repository는 생략해도 됩니다. Spring Data JPA가 자동으로 빈으로 등록함.

public interface SongRepository extends JpaRepository<Song, Long> {

    /**
     * 제목 또는 아티스트에 q가 (대소문자 구분 없이) 포함되는 곡을 페이징으로 검색
     */
    Page<Song> findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(
            String title, String artist, Pageable pageable
    );

    /**
     * 제목과 아티스트가 정확히 일치하는 곡을 찾기
     */
    Optional<Song> findByTitleAndArtist(String title, String artist);
}
