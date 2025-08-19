package com.example.musicrecommendation.service;

import com.example.musicrecommendation.config.SpotifyProperties;
import com.example.musicrecommendation.web.dto.spotify.SpotifyDto;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class SpotifyApiService {

    private final WebClient api;   // https://api.spotify.com/v1
    private final WebClient auth;  // https://accounts.spotify.com/api
    private final SpotifyProperties props;

    // 토큰 캐시
    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    public SpotifyApiService(WebClient.Builder builder, SpotifyProperties props) {
        this.props = props;
        this.api = builder
                .baseUrl(props.getApi().getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector())
                .build();
        this.auth = builder
                .baseUrl(props.getApi().getTokenUrl())
                .clientConnector(new ReactorClientHttpConnector())
                .build();
    }

    /** 외부에서 헬스체크로 사용 */
    public boolean testConnection() {
        try {
            // 간단한 no-op: 토큰만 갱신해 봄
            return getAccessToken() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /** Client Credentials Flow */
    private String getAccessToken() {
        if (cachedAccessToken != null && Instant.now().isBefore(tokenExpiry.minusSeconds(30))) {
            return cachedAccessToken;
        }

        String basic = Base64.getEncoder().encodeToString(
                (props.getClient().getId() + ":" + props.getClient().getSecret())
                        .getBytes(StandardCharsets.UTF_8)
        );

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        SpotifyToken token = auth.post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(h -> h.set("Authorization", "Basic " + basic))
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(SpotifyToken.class)
                .block();

        if (token == null || token.access_token == null) {
            throw new IllegalStateException("Spotify access token 발급 실패");
        }
        cachedAccessToken = token.access_token;
        // 만료 60초 전으로 여유
        tokenExpiry = Instant.now().plusSeconds(token.expires_in != null ? token.expires_in - 60 : 1800);
        return cachedAccessToken;
    }

    private WebClient authorized() {
        String token = getAccessToken();
        return api.mutate()
                .filter(ExchangeFilterFunctions
                        .basicAuthentication("", "")) // no-op, placeholder
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    /** 트랙 검색 */
    public Optional<SpotifyDto.TrackSearchResponse> searchTracks(String query, int limit) {
        Mono<SpotifyDto.TrackSearchResponse> mono = authorized().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("type", "track")
                        .queryParam("limit", Math.max(1, limit))
                        .build())
                .retrieve()
                .bodyToMono(SpotifyDto.TrackSearchResponse.class);

        return Optional.ofNullable(mono.block());
    }

    /** 장르 인기 트랙(간단 구현: genre 필터로 search) */
    public Optional<SpotifyDto.TrackSearchResponse> getPopularTracks(String genre, int limit) {
        Mono<SpotifyDto.TrackSearchResponse> mono = authorized().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", "genre:" + genre)
                        .queryParam("type", "track")
                        .queryParam("limit", Math.max(1, limit))
                        .build())
                .retrieve()
                .bodyToMono(SpotifyDto.TrackSearchResponse.class);

        return Optional.ofNullable(mono.block());
    }

    /** 오디오 특성 */
    public Optional<SpotifyDto.AudioFeatures> getAudioFeatures(String trackId) {
        Mono<SpotifyDto.AudioFeatures> mono = authorized().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/audio-features/{id}")
                        .build(trackId))
                .retrieve()
                .bodyToMono(SpotifyDto.AudioFeatures.class);

        return Optional.ofNullable(mono.block());
    }

    /** 내부 토큰 DTO */
    private static class SpotifyToken {
        public String access_token;
        public String token_type;
        public Integer expires_in;
        public String scope;
    }
}
