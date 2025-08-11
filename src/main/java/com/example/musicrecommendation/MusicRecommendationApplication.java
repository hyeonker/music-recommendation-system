package com.example.musicrecommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.cache.annotation.EnableCaching
public class MusicRecommendationApplication {
    public static void main(String[] args) {
        SpringApplication.run(MusicRecommendationApplication.class, args);
    }
}
