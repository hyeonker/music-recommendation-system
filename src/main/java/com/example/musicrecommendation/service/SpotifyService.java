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
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
        log.debug("토큰 체크: accessToken={}, 현재시간={}, 만료시간={}", 
            accessToken != null ? "exists" : "null", System.currentTimeMillis(), tokenExpiryTime);
        if (accessToken == null || System.currentTimeMillis() >= tokenExpiryTime) {
            log.info("토큰 갱신 필요 - 새로운 토큰 요청 중...");
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
     * 트랙 검색 (일반 검색용)
     */
    @Cacheable(
            value = "trackSearch",
            key = "#query + '|' + #limit",
            unless = "#result == null || #result.isEmpty()"
    )
    @Retry(name = "spotify", fallbackMethod = "searchTracksFallback")
    public List<TrackDto> searchTracks(String query, int limit) {
        return performTrackSearch(query, limit);
    }

    /**
     * 특정 아티스트의 곡 검색
     */
    @Cacheable(
            value = "artistTrackSearch",
            key = "#artistName + '|' + #query + '|' + #limit",
            unless = "#result == null || #result.isEmpty()"
    )
    @Retry(name = "spotify", fallbackMethod = "searchArtistTracksFallback")
    public List<TrackDto> searchArtistTracks(String artistName, String query, int limit) {
        ensureValidToken();
        
        log.info("🎯 아티스트별 곡 검색: 아티스트='{}', 곡='{}', 한계={}", artistName, query, limit);
        
        List<TrackDto> allResults = new ArrayList<>();
        
        // 1. 원본 검색어로 검색
        List<TrackDto> originalResults = performTrackSearch(query, 50);
        addArtistFilteredResults(allResults, originalResults, artistName);
        
        // 2. 검색어가 여러 단어인 경우, 각 단어별로도 검색
        String[] words = query.toLowerCase().split("\\s+");
        if (words.length > 1) {
            for (String word : words) {
                if (word.length() > 2) { // 3글자 이상인 단어만
                    List<TrackDto> wordResults = performTrackSearch(word, 30);
                    addArtistFilteredResults(allResults, wordResults, artistName);
                }
            }
        }
        
        // 3. 구두점 변형 검색 - 공백을 쉼표로 바꿔서 검색
        if (words.length > 1) {
            String punctuatedQuery = String.join(", ", words);
            List<TrackDto> punctResults = performTrackSearch(punctuatedQuery, 30);
            addArtistFilteredResults(allResults, punctResults, artistName);
        }
        
        // 4. 후보 아티스트의 인기곡에서 제목 기반 매칭
        try {
            List<TrackDto> topTracks = getArtistTopTracks(getArtistIdByName(artistName), "KR", 50);
            for (TrackDto track : topTracks) {
                String normalizedTrackTitle = normalizeTitle(track.getName());
                String normalizedQuery = normalizeTitle(query);
                
                if (normalizedTrackTitle.contains(normalizedQuery) || normalizedQuery.contains(normalizedTrackTitle)) {
                    boolean alreadyExists = allResults.stream().anyMatch(existing -> existing.getId().equals(track.getId()));
                    if (!alreadyExists) {
                        allResults.add(track);
                        log.debug("✅ 인기곡에서 제목 매칭: '{}' ← '{}'", track.getName(), query);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("인기곡 매칭 실패: {}", e.getMessage());
        }
        
        log.info("🎯 아티스트 필터링 완료: 총 {}개 결과", allResults.size());
        
        return allResults.stream().limit(limit).collect(Collectors.toList());
    }
    
    private void addArtistFilteredResults(List<TrackDto> allResults, List<TrackDto> newResults, String artistName) {
        for (TrackDto track : newResults) {
            // 이미 결과에 있는지 확인
            boolean alreadyExists = allResults.stream().anyMatch(existing -> existing.getId().equals(track.getId()));
            if (alreadyExists) continue;
            
            // 아티스트 매칭 확인
            if (track.getArtists() != null) {
                boolean matchesArtist = track.getArtists().stream()
                    .anyMatch(artist -> artist.getName().equalsIgnoreCase(artistName));
                
                if (matchesArtist) {
                    allResults.add(track);
                    log.debug("✅ 매칭된 곡 추가: '{}' by {}", track.getName(), 
                        track.getArtists().stream().map(a -> a.getName()).collect(Collectors.joining(", ")));
                }
            }
        }
    }
    
    /**
     * 아티스트 이름으로 아티스트 ID 찾기 (간단한 캐시)
     */
    private String getArtistIdByName(String artistName) {
        List<ArtistDto> artists = searchArtists(artistName, 1);
        return artists.isEmpty() ? null : artists.get(0).getId();
    }
    
    /**
     * 제목 정규화 (구두점 제거, 소문자 변환)
     */
    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase()
            .replaceAll("[\\p{Punct}]", " ")  // 모든 구두점을 공백으로
            .replaceAll("\\s+", " ")         // 연속 공백을 하나로
            .trim();
    }
    
    
    /**
     * 중복되지 않는 결과만 추가
     */
    private void addUniqueResults(List<TrackDto> allResults, List<TrackDto> newResults, int maxSize) {
        for (TrackDto track : newResults) {
            if (allResults.size() >= maxSize) break;
            
            boolean exists = allResults.stream().anyMatch(existing -> 
                existing.getId().equals(track.getId()) || 
                (existing.getName().equalsIgnoreCase(track.getName()) && 
                 existing.getArtists().stream().anyMatch(existingArtist ->
                    track.getArtists().stream().anyMatch(newArtist ->
                        existingArtist.getName().equalsIgnoreCase(newArtist.getName())
                    )
                 ))
            );
            
            if (!exists) {
                allResults.add(track);
            }
        }
    }
    
    
    private List<TrackDto> performTrackSearch(String query, int limit) {
        ensureValidToken();
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            String url = UriComponentsBuilder.fromHttpUrl(spotifyConfig.getApi().getBaseUrl() + "/search")
                    .queryParam("q", query)
                    .queryParam("type", "track")
                    .queryParam("limit", limit)
                    .toUriString();

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.debug("Spotify 트랙 검색 호출: q='{}', limit={}", query, limit);
            log.info("🔗 실제 요청 URL: {}", url);

            ResponseEntity<SpotifySearchResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, SpotifySearchResponse.class
            );

            if (response.getBody() != null && response.getBody().getTracks() != null) {
                List<TrackDto> out = convertToTrackDtos(response.getBody().getTracks().getItems());
                log.debug("Spotify 트랙 검색 결과: {}건", out.size());
                
                // 처음 3개 곡 이름 로깅
                for (int i = 0; i < Math.min(3, out.size()); i++) {
                    TrackDto track = out.get(i);
                    String artistName = track.getArtists() != null && !track.getArtists().isEmpty() ? 
                        track.getArtists().get(0).getName() : "Unknown";
                    log.warn("📀 검색결과 #{}: '{}' by {} (ID: {})", i+1, track.getName(), artistName, track.getId());
                }
                
                // 특별히 5h4y42RUKwYKYWgutNwvKP ID를 찾아보기
                boolean foundTarget = out.stream().anyMatch(track -> "5h4y42RUKwYKYWgutNwvKP".equals(track.getId()));
                log.warn("🎯 Target song (5h4y42RUKwYKYWgutNwvKP) found in results: {}", foundTarget);
                
                return out;
            }

            return new ArrayList<>();

        } catch (HttpStatusCodeException e) {
            int code = e.getStatusCode().value();
            if (e.getStatusCode().is5xxServerError() || code == 429) {
                log.warn("Spotify 일시적 오류 감지(상태 {}), 재시도 대상: {}", code, e.getMessage());
                throw new TransientSpotifyException("Transient HTTP " + code, e);
            }
            log.error("Spotify 클라이언트 오류(상태 {}): {}", code, e.getResponseBodyAsString());
            return new ArrayList<>(); // 개별 검색 실패 시 빈 결과 반환

        } catch (ResourceAccessException e) {
            log.warn("Spotify 네트워크 오류: {}", e.getMessage());
            return new ArrayList<>(); // 개별 검색 실패 시 빈 결과 반환

        } catch (Exception e) {
            log.error("트랙 검색 실패: {}", e.getMessage());
            return new ArrayList<>(); // 개별 검색 실패 시 빈 결과 반환
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
     * 트랙 검색 Retry 실패 시 폴백
     */
    public List<TrackDto> searchTracksFallback(String query, int limit, Throwable t) {
        log.warn("searchTracks 폴백 동작: q='{}', limit={}, cause={}", query, limit, t.toString());
        return List.of();
    }

    /**
     * 아티스트별 곡 검색 Retry 실패 시 폴백
     */
    public List<TrackDto> searchArtistTracksFallback(String artistName, String query, int limit, Throwable t) {
        log.warn("searchArtistTracks 폴백 동작: artist='{}', q='{}', limit={}, cause={}", artistName, query, limit, t.toString());
        return List.of();
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
     * Spotify Track ID로 직접 곡 정보 가져오기
     */
    private TrackDto getTrackById(String trackId) {
        ensureValidToken();
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            
            String url = spotifyConfig.getApi().getBaseUrl() + "/tracks/" + trackId;
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<SpotifyTrack> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, SpotifyTrack.class);
            
            if (response.getBody() != null) {
                List<SpotifyTrack> tracks = Collections.singletonList(response.getBody());
                List<TrackDto> trackDtos = convertToTrackDtos(tracks);
                return trackDtos.isEmpty() ? null : trackDtos.get(0);
            }
        } catch (Exception e) {
            log.warn("Track ID로 곡 가져오기 실패 ({}): {}", trackId, e.getMessage());
        }
        
        return null;
    }

    /**
     * Spotify Track → TrackDto 매핑
     */
    private List<TrackDto> convertToTrackDtos(List<SpotifyTrack> spotifyTracks) {
        List<TrackDto> trackDtos = new ArrayList<>();

        for (SpotifyTrack spotifyTrack : spotifyTracks) {
            TrackDto dto = new TrackDto();
            dto.setId(spotifyTrack.getId());
            dto.setName(spotifyTrack.getName());
            dto.setHref(spotifyTrack.getHref());
            dto.setPopularity(spotifyTrack.getPopularity());
            dto.setDurationMs(spotifyTrack.getDurationMs());
            dto.setExplicit(spotifyTrack.getExplicit());
            dto.setPreviewUrl(spotifyTrack.getPreviewUrl());
            dto.setTrackNumber(spotifyTrack.getTrackNumber());
            dto.setType(spotifyTrack.getType());
            dto.setUri(spotifyTrack.getUri());
            dto.setIsLocal(spotifyTrack.getIsLocal());

            if (spotifyTrack.getExternalUrls() != null) {
                dto.setSpotifyUrl(spotifyTrack.getExternalUrls().getSpotify());
            }

            // 아티스트 정보 변환 (트랙 인기도 기반 아티스트 인기도 추정)
            if (spotifyTrack.getArtists() != null) {
                List<ArtistDto> artists = convertToArtistDtos(spotifyTrack.getArtists());
                
                // 트랙 인기도를 기반으로 아티스트 인기도 추정
                // 일반적으로 아티스트 인기도는 트랙 인기도보다 조금 높거나 비슷함
                Integer trackPopularity = spotifyTrack.getPopularity();
                if (trackPopularity != null && trackPopularity > 0) {
                    // 트랙 인기도를 기반으로 아티스트 인기도 추정: 최소 트랙 인기도 이상, 최대 100
                    int estimatedArtistPop = Math.min(100, trackPopularity + 10);
                    
                    for (ArtistDto artist : artists) {
                        if (artist.getPopularity() == null || artist.getPopularity() == 0) {
                            artist.setPopularity(estimatedArtistPop);
                        }
                    }
                    log.debug("아티스트 인기도 추정: 트랙 {}점 → 아티스트 {}점 (보정 후)", trackPopularity, 
                            artists.stream().mapToInt(a -> a.getPopularity() != null ? a.getPopularity() : 0).max().orElse(0));
                }
                
                dto.setArtists(artists);
            }

            // 앨범 정보 변환
            if (spotifyTrack.getAlbum() != null) {
                AlbumDto albumDto = new AlbumDto();
                albumDto.setId(spotifyTrack.getAlbum().getId());
                albumDto.setName(spotifyTrack.getAlbum().getName());
                albumDto.setAlbumType(spotifyTrack.getAlbum().getAlbumType());
                albumDto.setTotalTracks(spotifyTrack.getAlbum().getTotalTracks());
                albumDto.setReleaseDate(spotifyTrack.getAlbum().getReleaseDate());
                albumDto.setReleaseDatePrecision(spotifyTrack.getAlbum().getReleaseDatePrecision());
                albumDto.setImages(spotifyTrack.getAlbum().getImages());
                
                if (spotifyTrack.getAlbum().getArtists() != null) {
                    albumDto.setArtists(convertToArtistDtos(spotifyTrack.getAlbum().getArtists()));
                }
                
                dto.setAlbum(albumDto);
                
                // 트랙 이미지는 앨범 이미지로 설정
                if (spotifyTrack.getAlbum().getImages() != null && !spotifyTrack.getAlbum().getImages().isEmpty()) {
                    dto.setImage(spotifyTrack.getAlbum().getImages().get(0).getUrl());
                }
            }

            trackDtos.add(dto);
        }

        return trackDtos;
    }

    /**
     * 아티스트의 인기 곡 목록 가져오기
     */
    @Retry(name = "spotify")
    public List<TrackDto> getArtistTopTracks(String artistId, String market, int limit) {
        ensureValidToken();
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            
            String url = String.format("%s/artists/%s/top-tracks?market=%s", 
                spotifyConfig.getApi().getBaseUrl(), artistId, market);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<SpotifyTopTracksResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, SpotifyTopTracksResponse.class);
            
            if (response.getBody() != null && response.getBody().getTracks() != null) {
                List<SpotifyTrack> tracks = response.getBody().getTracks().stream()
                    .limit(limit)
                    .collect(Collectors.toList());
                
                log.info("아티스트 {} 인기곡 {}개 조회 완료", artistId, tracks.size());
                return convertToTrackDtos(tracks);
            }
            
            return List.of();
            
        } catch (Exception e) {
            log.error("아티스트 {} 인기곡 조회 실패: {}", artistId, e.getMessage());
            return List.of();
        }
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
