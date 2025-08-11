package com.example.musicrecommendation.service;

import com.example.musicrecommendation.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SongService {
    private final SongRepository repo;
    public SongService(SongRepository repo) { this.repo = repo; }

    @Transactional public Song add(String title, String artist) {
        return repo.save(new Song(title, artist));
    }
    @Transactional(readOnly = true) public List<Song> top() {
        return repo.findTop5ByOrderByIdDesc();
    }
}
