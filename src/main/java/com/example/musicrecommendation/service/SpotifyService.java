package com.example.musicrecommendation.service;

import com.example.musicrecommendation.config.SpotifyConfig;
import com.example.musicrecommendation.web.dto.spotify.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class SpotifyService {

    @Autowired
    private SpotifyConfig spotifyConfig;

    @Autowired
    private RestTemplate restTemplate;

    private String accessToken;
    private long tokenExpiryTime;

    /**
     * Spotify API 액세스 토큰 획득
     */
    private void authenticateWithSpotify() {
        try {
            // Client Credentials 방식으로 토큰 요청
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
                this.tokenExpiryTime = System.currentTimeMillis() + (response.getBody().getExpiresIn() * 1000);
                System.out.println("Spotify 토큰 획득 성공!");
            }

        } catch (Exception e) {
            System.err.println("Spotify 인증 실패: " + e.getMessage());
            throw new RuntimeException("Spotify 인증에 실패했습니다.", e);
        }
    }

    /**
     * 토큰이 유효한지 확인하고 필요시 갱신
     */
    private void ensureValidToken() {
        if (accessToken == null || System.currentTimeMillis() >= tokenExpiryTime) {
            authenticateWithSpotify();
        }
    }

    /**
     * 아티스트 검색
     */
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

            ResponseEntity<SpotifySearchResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, SpotifySearchResponse.class
            );

            if (response.getBody() != null && response.getBody().getArtists() != null) {
                return convertToArtistDtos(response.getBody().getArtists().getItems());
            }

            return new ArrayList<>();

        } catch (Exception e) {
            System.err.println("아티스트 검색 실패: " + e.getMessage());
            throw new RuntimeException("아티스트 검색에 실패했습니다.", e);
        }
    }

    /**
     * Spotify Artist를 ArtistDto로 변환
     */
    private List<ArtistDto> convertToArtistDtos(List<SpotifyArtist> spotifyArtists) {
        List<ArtistDto> artistDtos = new ArrayList<>();

        for (SpotifyArtist spotifyArtist : spotifyArtists) {
            ArtistDto dto = new ArtistDto();
            dto.setId(spotifyArtist.getId());
            dto.setName(spotifyArtist.getName());
            dto.setGenres(spotifyArtist.getGenres());
            dto.setPopularity(spotifyArtist.getPopularity());

            // 외부 URL 설정
            if (spotifyArtist.getExternalUrls() != null) {
                dto.setSpotifyUrl(spotifyArtist.getExternalUrls().getSpotify());
            }

            // 팔로워 수 설정
            if (spotifyArtist.getFollowers() != null) {
                dto.setFollowers(spotifyArtist.getFollowers().getTotal());
            }

            // 이미지 URL 설정 (가장 큰 이미지 선택)
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