package com.example.musicrecommendation;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class ITBase {

    // Postgres 16 컨테이너 (테스트 시작 시 자동 가동)
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("music")
                    .withUsername("music")
                    .withPassword("music");

    @BeforeAll
    static void startContainer() {
        if (!POSTGRES.isRunning()) POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        // 테스트에선 캐시 끄고 싶으면 주석 해제
        // r.add("spring.cache.type", () -> "none");
    }
}
