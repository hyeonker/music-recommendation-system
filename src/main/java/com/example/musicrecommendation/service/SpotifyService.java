package com.example.musicrecommendation.service;

import com.example.musicrecommendation.config.SpotifyConfig;
import com.example.musicrecommendation.web.dto.spotify.*;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class SpotifyService {

    private static final Logger log = LoggerFactory.getLogger(SpotifyService.class);

    @Autowired
    private SpotifyConfig spotifyConfig;

    @Autowired
    private RestTemplate restTemplate;

    private String accessToken;
    private long tokenExpiryTime;

    /**
     * Spotify API 액세스 토큰 획득 (Client Credentials)
     */
    private void authenticateWithSpotify() {
        try {
            String credentials = spotifyConfig.getClient().getId() + ":" + spotifyConfig.getClient().getSecret();
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encodedCredentials);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<SpotifyTokenResponse> response = restTemplate.postForEntity(
                    spotifyConfig.getApi().getAuthUrl(),
                    request,
                    SpotifyTokenResponse.class
            );

            if (response.getBody() != null) {
                this.accessToken = response.getBody().getAccessToken();
                this.tokenExpiryTime = System.currentTimeMillis() + (response.getBody().getExpiresIn() * 1000L);
                log.debug("Spotify 토큰 획득 성공, 만료(초): {}", response.getBody().getExpiresIn());
            }

        } catch (Exception e) {
            log.error("Spotify 인증 실패: {}", e.getMessage());
            throw new RuntimeException("Spotify 인증에 실패했습니다.", e);
        }
    }

    /**
     * 토큰 유효성 체크 및 필요 시 갱신
     */
    private void ensureValidToken() {
        if (accessToken == null || System.currentTimeMillis() >= tokenExpiryTime) {
            authenticateWithSpotify();
        }
    }

    /**
     * 아티스트 검색
     * - 캐시: 동일 (query, limit) 10분간
     * - 재시도: 5xx/429/네트워크 등 "일시적 오류"는 최대 3회 (지수 백오프)
     */
    @Cacheable(
            value = "artistSearch",
            key = "#query + '|' + #limit",
            unless = "#result == null || #result.isEmpty()"
    )
    @Retry(name = "spotify", fallbackMethod = "searchFallback")
    public List<ArtistDto> searchArtists(String query, int limit) {
        ensureValidToken();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            String url = UriComponentsBuilder.fromHttpUrl(spotifyConfig.getApi().getBaseUrl() + "/search")
                    .queryParam("q", query)
                    .queryParam("type", "artist")
                    .queryParam("limit", limit)
                    .toUriString();

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.debug("Spotify 검색 호출: q='{}', limit={}", query, limit);

            ResponseEntity<SpotifySearchResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, SpotifySearchResponse.class
            );

            if (response.getBody() != null && response.getBody().getArtists() != null) {
                List<ArtistDto> out = convertToArtistDtos(response.getBody().getArtists().getItems());
                log.debug("Spotify 검색 결과: {}건", out.size());
                return out;
            }

            return new ArrayList<>();

        } catch (HttpStatusCodeException e) {
            // Spring 6: getStatusCode()는 HttpStatusCode (value()로 비교)
            int code = e.getStatusCode().value();
            // 5xx 또는 429는 "일시적 오류"로 간주 → 재시도 대상
            if (e.getStatusCode().is5xxServerError() || code == 429) {
                log.warn("Spotify 일시적 오류 감지(상태 {}), 재시도 대상: {}", code, e.getMessage());
                throw new TransientSpotifyException("Transient HTTP " + code, e);
            }
            // 그 외 4xx는 즉시 실패
            log.error("Spotify 클라이언트 오류(상태 {}): {}", code, e.getResponseBodyAsString());
            throw new RuntimeException("아티스트 검색에 실패했습니다. (status=" + code + ")", e);

        } catch (ResourceAccessException e) {
            // 타임아웃/네트워크 오류 → 재시도 대상
            log.warn("Spotify 네트워크 오류, 재시도 대상: {}", e.getMessage());
            throw new TransientSpotifyException("Network error", e);

        } catch (Exception e) {
            log.error("아티스트 검색 실패: {}", e.getMessage());
            throw new RuntimeException("아티스트 검색에 실패했습니다.", e);
        }
    }

    /**
     * Retry 실패 시 폴백: 안전하게 빈 리스트 반환 (UI가 graceful 하게 처리)
     */
    public List<ArtistDto> searchFallback(String query, int limit, Throwable t) {
        log.warn("searchArtists 폴백 동작: q='{}', limit={}, cause={}", query, limit, t.toString());
        return List.of(); // 필요 시 캐시된 마지막 성공 결과를 반환하도록 확장 가능
    }

    /**
     * Spotify Artist → ArtistDto 매핑
     */
    private List<ArtistDto> convertToArtistDtos(List<SpotifyArtist> spotifyArtists) {
        List<ArtistDto> artistDtos = new ArrayList<>();

        for (SpotifyArtist spotifyArtist : spotifyArtists) {
            ArtistDto dto = new ArtistDto();
            dto.setId(spotifyArtist.getId());
            dto.setName(spotifyArtist.getName());
            dto.setGenres(spotifyArtist.getGenres());
            dto.setPopularity(spotifyArtist.getPopularity());

            if (spotifyArtist.getExternalUrls() != null) {
                dto.setSpotifyUrl(spotifyArtist.getExternalUrls().getSpotify());
            }
            if (spotifyArtist.getFollowers() != null) {
                dto.setFollowers(spotifyArtist.getFollowers().getTotal());
            }
            if (spotifyArtist.getImages() != null && !spotifyArtist.getImages().isEmpty()) {
                dto.setImage(spotifyArtist.getImages().get(0).getUrl());
            }

            artistDtos.add(dto);
        }

        return artistDtos;
    }

    /**
     * 서비스 상태 확인
     */
    public String getHealthStatus() {
        try {
            ensureValidToken();
            return "Spotify API 연결 정상";
        } catch (Exception e) {
            return "Spotify API 연결 실패: " + e.getMessage();
        }
    }
}
