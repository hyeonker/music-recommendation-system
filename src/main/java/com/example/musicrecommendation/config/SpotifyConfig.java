// src/main/java/com/example/musicrecommendation/config/SpotifyConfig.java
package com.example.musicrecommendation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spotify")
public class SpotifyConfig {

    private final Client client = new Client();
    private final Api api = new Api();

    public Client getClient() { return client; }
    public Api getApi() { return api; }

    public static class Client {
        private String id;
        private String secret;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
    }

    public static class Api {
        // 기본값은 필요 시 조정
        private String baseUrl = "https://api.spotify.com/v1";
        private String authUrl = "https://accounts.spotify.com/api/token";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getAuthUrl() { return authUrl; }
        public void setAuthUrl(String authUrl) { this.authUrl = authUrl; }
    }
}
