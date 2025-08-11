package com.example.musicrecommendation.web;

import com.example.musicrecommendation.service.SongService;
import com.example.musicrecommendation.web.dto.SongCreateRequest;
import com.example.musicrecommendation.web.dto.SongResponse;
import com.example.musicrecommendation.web.dto.SongUpdateRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/songs", produces = MediaType.APPLICATION_JSON_VALUE)
public class SongController {

    private final SongService svc;

    public SongController(SongService svc) {
        this.svc = svc;
    }

    /** 곡 등록 (JSON 바디 + 검증) → DTO 반환, 201 Created */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SongResponse> add(@RequestBody @Valid SongCreateRequest req) {
        var saved = svc.add(req.title(), req.artist());
        return ResponseEntity.status(HttpStatus.CREATED).body(SongResponse.from(saved));
    }

    /** TOP 10 (캐시 적용) */
    @GetMapping("/top")
    public List<SongResponse> top() {
        return svc.topDto();
    }

    /** 검색 + 페이징 (q, page, size, sort) → DTO Page */
    @GetMapping
    public Page<SongResponse> page(
            @RequestParam(required = false) String q,
            @ParameterObject
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return svc.pageDto(q, pageable);
    }

    /** 수정 (낙관적 락: version 필수) */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SongResponse update(@PathVariable Long id, @RequestBody @Valid SongUpdateRequest req) {
        return svc.update(id, req);
    }

    /** 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        svc.delete(id);
        return ResponseEntity.noContent().build();
    }
}
