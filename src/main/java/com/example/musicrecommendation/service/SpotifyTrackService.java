package com.example.musicrecommendation.service;

import com.example.musicrecommendation.config.SpotifyConfig;
import com.example.musicrecommendation.web.dto.spotify.SpotifyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * WebFlux 없이 RestTemplate로 구현한 트랙/오디오 특성용 서비스
 * - Client Credentials 토큰 캐시 포함
 */
@Service
public class SpotifyTrackService {

    private static final Logger log = LoggerFactory.getLogger(SpotifyTrackService.class);

    private final RestTemplate restTemplate;
    private final SpotifyConfig props;

    private volatile String accessToken;
    private volatile long tokenExpiryMillis;

    public SpotifyTrackService(RestTemplate restTemplate, SpotifyConfig props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    /** 외부에서 헬스체크로 사용 */
    public boolean testConnection() {
        try {
            ensureToken();
            return true;
        } catch (Exception e) {
            log.warn("Spotify 연결 점검 실패: {}", e.getMessage());
            return false;
        }
    }

    private void ensureToken() {
        long now = System.currentTimeMillis();
        if (accessToken == null || now >= tokenExpiryMillis) {
            requestToken();
        }
    }

    private void requestToken() {
        try {
            String basic = Base64.getEncoder().encodeToString(
                    (props.getClient().getId() + ":" + props.getClient().getSecret())
                            .getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + basic);

            String body = "grant_type=client_credentials";

            HttpEntity<String> req = new HttpEntity<>(body, headers);

            ResponseEntity<TokenResp> resp = restTemplate.postForEntity(
                    props.getApi().getAuthUrl(),   // 예: https://accounts.spotify.com/api/token
                    req,
                    TokenResp.class
            );

            if (resp.getBody() == null || resp.getBody().access_token == null) {
                throw new IllegalStateException("Spotify access token 발급 실패");
            }
            this.accessToken = resp.getBody().access_token;
            int expires = resp.getBody().expires_in != null ? resp.getBody().expires_in : 3600;
            // 60초 여유
            this.tokenExpiryMillis = System.currentTimeMillis() + (expires - 60) * 1000L;
            log.debug("Spotify token OK, expires={}s", expires);
        } catch (Exception e) {
            throw new RuntimeException("Spotify 토큰 발급 실패", e);
        }
    }

    private HttpHeaders bearer() {
        ensureToken();
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer " + accessToken);
        return h;
    }

    /** 트랙 검색 */
    public Optional<SpotifyDto.TrackSearchResponse> searchTracks(String query, int limit) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(props.getApi().getBaseUrl() + "/search")
                    .queryParam("q", query)
                    .queryParam("type", "track")
                    .queryParam("limit", Math.max(1, limit))
                    .toUriString();

            ResponseEntity<SpotifyDto.TrackSearchResponse> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(bearer()), SpotifyDto.TrackSearchResponse.class
            );
            return Optional.ofNullable(resp.getBody());
        } catch (Exception e) {
            log.error("searchTracks 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** 장르 인기 트랙(간단 구현: genre 필터로 search) */
    public Optional<SpotifyDto.TrackSearchResponse> getPopularTracks(String genre, int limit) {
        return searchTracks("genre:" + genre, limit);
    }

    /** 오디오 특성 */
    public Optional<SpotifyDto.AudioFeatures> getAudioFeatures(String trackId) {
        try {
            String url = props.getApi().getBaseUrl() + "/audio-features/" + trackId;
            ResponseEntity<SpotifyDto.AudioFeatures> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(bearer()), SpotifyDto.AudioFeatures.class
            );
            return Optional.ofNullable(resp.getBody());
        } catch (Exception e) {
            log.error("getAudioFeatures 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // 내부 토큰 DTO
    private static class TokenResp {
        public String access_token;
        public String token_type;
        public Integer expires_in;
        public String scope;
    }
}
