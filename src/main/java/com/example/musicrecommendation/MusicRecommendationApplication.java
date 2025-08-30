// src/main/java/com/example/musicrecommendation/MusicRecommendationApplication.java
package com.example.musicrecommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaAuditing
@SpringBootApplication
public class MusicRecommendationApplication {
    public static void main(String[] args) {
        SpringApplication.run(MusicRecommendationApplication.class, args);
    }
}
