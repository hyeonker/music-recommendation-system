package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.Song;
import com.example.musicrecommendation.domain.SongRepository;
import com.example.musicrecommendation.web.dto.SongResponse;
import com.example.musicrecommendation.web.dto.SongUpdateRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class SongService {
    private final SongRepository repo;

    public SongService(SongRepository repo) { this.repo = repo; }

    /** 곡 등록: 즉시 flush 해서 version(=0)까지 채워 응답에 반영 */
    @Transactional
    @CacheEvict(value = "topSongs", allEntries = true)
    public Song add(String title, String artist) {
        // ✅ save → saveAndFlush 로 변경
        return repo.saveAndFlush(new Song(title, artist));
    }

    /** TOP 10 (캐시) */
    @Cacheable("topSongs")
    public List<SongResponse> topDto() {
        return repo.findAll(Sort.by("id").descending())
                .stream().limit(10).map(SongResponse::from).toList();
    }

    /** 검색 페이지 */
    public Page<Song> page(String q, Pageable pageable) {
        if (q == null || q.isBlank()) return repo.findAll(pageable);
        return repo.findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(q, q, pageable);
    }

    public Page<SongResponse> pageDto(String q, Pageable pageable) {
        return page(q, pageable).map(SongResponse::from);
    }

    /** 수정: 낙관적 락 체크 + 플러시(증가된 version을 응답에 반영) */
    @Transactional
    @CacheEvict(value = "topSongs", allEntries = true)
    public SongResponse update(Long id, SongUpdateRequest req) {
        Song s = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("song not found: " + id));

        // 클라이언트가 전달한 version과 현재 버전이 다르면 충돌
        if (!Objects.equals(req.version(), s.getVersion())) {
            throw new OptimisticLockingFailureException("stale version: " + req.version());
        }

        s.change(req.title(), req.artist());

        // ✅ flush 해서 증가된 version이 바로 반영되도록
        repo.saveAndFlush(s); // (또는 repo.flush(); 도 가능)

        return SongResponse.from(s);
    }

    /** 삭제 */
    @Transactional
    @CacheEvict(value = "topSongs", allEntries = true)
    public void delete(Long id) {
        repo.deleteById(id);
    }
}
