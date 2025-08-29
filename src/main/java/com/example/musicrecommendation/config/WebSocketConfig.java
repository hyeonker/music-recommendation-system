// src/main/java/com/example/musicrecommendation/config/WebSocketConfig.java
package com.example.musicrecommendation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 프론트에서 SockJS('/ws')를 사용하므로 반드시 withSockJS() 필요
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        // 만약 네이티브 WebSocket도 같이 열고 싶다면(선택):
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 프론트가 destination:/app/...으로 보내므로 prefix는 /app
        registry.setApplicationDestinationPrefixes("/app");
        // 서버가 방송하는 채널(prefix)들
        registry.enableSimpleBroker("/topic", "/queue");
        // /user/queue/... 구독용
        registry.setUserDestinationPrefix("/user");
    }
}
