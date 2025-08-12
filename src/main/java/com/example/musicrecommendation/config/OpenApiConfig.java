package com.example.musicrecommendation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Music Recommendation API")
                        .version("v1")
                        .description("Songs CRUD + search (Spring Boot, JPA, Flyway, Caffeine cache)"))
                .addServersItem(new Server().url("http://localhost:9090"));
    }
}
