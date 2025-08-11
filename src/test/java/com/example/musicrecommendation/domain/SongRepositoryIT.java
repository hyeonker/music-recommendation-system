package com.example.musicrecommendation.domain;

import com.example.musicrecommendation.ITBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class SongRepositoryIT extends ITBase {

    @Autowired
    SongRepository repo;

    @Test
    void save_and_query() {
        // ✅ 시드와 절대 겹치지 않도록 랜덤 타이틀 사용
        var title = "Time IT " + UUID.randomUUID();
        var s = repo.save(new Song(title, "Hans Zimmer"));
        assertThat(s.getId()).isNotNull();

        var page = repo.findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(
                "time", "time", org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(Song::getTitle)
                .contains(title);
    }
}
