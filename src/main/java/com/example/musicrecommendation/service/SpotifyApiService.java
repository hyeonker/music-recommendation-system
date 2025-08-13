package com.example.musicrecommendation.service;

import com.example.musicrecommendation.web.dto.SpotifyDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Spotify API 클라이언트 서비스
 */
@Service
public class SpotifyApiService {

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    @Value("${spotify.api.base-url}")
    private String apiBaseUrl;

    @Value("${spotify.accounts.base-url}")
    private String accountsBaseUrl;

    private final WebClient webClient;
    private String accessToken;
    private long tokenExpiryTime;

    public SpotifyApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Access Token 획득
     */
    private String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return accessToken;
        }

        try {
            String credentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

            SpotifyDto.TokenResponse response = webClient.post()
                    .uri(accountsBaseUrl + "/api/token")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body(BodyInserters.fromFormData("grant_type", "client_credentials"))
                    .retrieve()
                    .bodyToMono(SpotifyDto.TokenResponse.class)
                    .block();

            if (response != null && response.getAccessToken() != null) {
                this.accessToken = response.getAccessToken();
                this.tokenExpiryTime = System.currentTimeMillis() + (response.getExpiresIn() * 1000L) - 60000; // 1분 여유
                return accessToken;
            }
        } catch (Exception e) {
            System.err.println("Spotify token 획득 실패: " + e.getMessage());
        }

        return null;
    }

    /**
     * 곡 검색
     */
    public Optional<SpotifyDto.TrackSearchResponse> searchTracks(String query, int limit) {
        String token = getAccessToken();
        if (token == null) {
            return Optional.empty();
        }

        try {
            SpotifyDto.TrackSearchResponse response = webClient.get()
                    .uri(apiBaseUrl + "/search?q={query}&type=track&limit={limit}", query, limit)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(SpotifyDto.TrackSearchResponse.class)
                    .block();

            return Optional.ofNullable(response);
        } catch (Exception e) {
            System.err.println("Spotify 검색 실패: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 오디오 특성 분석
     */
    public Optional<SpotifyDto.AudioFeatures> getAudioFeatures(String trackId) {
        String token = getAccessToken();
        if (token == null) {
            return Optional.empty();
        }

        try {
            SpotifyDto.AudioFeatures response = webClient.get()
                    .uri(apiBaseUrl + "/audio-features/{id}", trackId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(SpotifyDto.AudioFeatures.class)
                    .block();

            return Optional.ofNullable(response);
        } catch (Exception e) {
            System.err.println("Spotify 오디오 특성 분석 실패: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 인기 곡 추천
     */
    public Optional<SpotifyDto.TrackSearchResponse> getPopularTracks(String genre, int limit) {
        String query = genre != null ? "genre:" + genre : "year:2024";
        return searchTracks(query, limit);
    }

    /**
     * 비슷한 곡 추천 (간단 버전)
     */
    public Optional<SpotifyDto.TrackSearchResponse> getSimilarTracks(String artistName, int limit) {
        if (artistName == null || artistName.trim().isEmpty()) {
            return getPopularTracks("pop", limit);
        }

        String query = "artist:" + artistName;
        return searchTracks(query, limit);
    }

    /**
     * 연결 테스트
     */
    public boolean testConnection() {
        String token = getAccessToken();
        return token != null;
    }

    /**
     * API 상태 정보
     */
    public Object getApiStatus() {
        boolean connected = testConnection();

        return new Object() {
            public final boolean isConnected = connected;
            public final String status = connected ? "✅ Spotify API 연결 성공" : "❌ Spotify API 연결 실패";
            public final String clientId = SpotifyApiService.this.clientId != null ?
                    SpotifyApiService.this.clientId.substring(0, Math.min(8, SpotifyApiService.this.clientId.length())) + "..." : "설정되지 않음";
            public final String apiBaseUrl = SpotifyApiService.this.apiBaseUrl;
        };
    }
}