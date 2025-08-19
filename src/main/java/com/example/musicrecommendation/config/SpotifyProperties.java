package com.example.musicrecommendation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spotify")
public class SpotifyProperties {

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
        /** ex) https://api.spotify.com/v1 */
        private String baseUrl;
        /** ex) https://accounts.spotify.com/api */
        private String tokenUrl;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getTokenUrl() { return tokenUrl; }
        public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }
    }
}
