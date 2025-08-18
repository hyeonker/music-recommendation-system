package com.example.musicrecommendation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spotify")
public class SpotifyConfig {

    private Client client = new Client();
    private Api api = new Api();

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public Api getApi() { return api; }
    public void setApi(Api api) { this.api = api; }

    public static class Client {
        private String id;
        private String secret;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
    }

    public static class Api {
        private String baseUrl;
        private String authUrl;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getAuthUrl() { return authUrl; }
        public void setAuthUrl(String authUrl) { this.authUrl = authUrl; }
    }
}