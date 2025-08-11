package com.example.musicrecommendation.web;

import com.example.musicrecommendation.ITBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SongControllerIT extends ITBase {

    @Autowired
    MockMvc mvc;

    @Test
    void add_and_page_and_top() throws Exception {
        // 매번 유니크한 제목으로 중복(409) 방지
        String title = "So What " + UUID.randomUUID();
        String body = """
            {"title":"%s","artist":"Miles Davis"}
        """.formatted(title);

        // 1) 곡 등록 → 201
        mvc.perform(post("/api/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.artist").value("Miles Davis"))
                .andExpect(jsonPath("$.version").isNumber());

        // 2) 페이지 조회 → 해당 제목이 포함되어 있는지만 확인
        mvc.perform(get("/api/songs")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,desc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", hasItem(title)));

        // 3) TOP 조회 → 포함 여부만 확인
        mvc.perform(get("/api/songs/top"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem(title)));
    }

    @Test
    void validate_error_when_blank_title() throws Exception {
        mvc.perform(post("/api/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"title":"","artist":""}
                """))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
