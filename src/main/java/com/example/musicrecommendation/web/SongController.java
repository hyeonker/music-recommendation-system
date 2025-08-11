package com.example.musicrecommendation.web;

import com.example.musicrecommendation.domain.Song;
import com.example.musicrecommendation.service.SongService;
import com.example.musicrecommendation.web.dto.SongCreateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/songs")
public class SongController {

    private final SongService svc;

    public SongController(SongService svc) { this.svc = svc; }

    // JSON 바디 + 검증
    @PostMapping
    public Song add(@RequestBody @Valid SongCreateRequest req) {
        return svc.add(req.title(), req.artist());
    }

    @GetMapping("/top")
    public List<Song> top() { return svc.top(); }
}
