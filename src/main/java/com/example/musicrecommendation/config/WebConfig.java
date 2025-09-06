package com.example.musicrecommendation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders(
                    "X-Refresh-Used", 
                    "X-Refresh-Remaining", 
                    "X-Refresh-Max", 
                    "X-Refresh-Reset-Date",
                    "X-Hourly-Used",
                    "X-Hourly-Remaining", 
                    "X-Hourly-Max"
                );
    }
}