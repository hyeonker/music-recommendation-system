// src/main/java/com/example/musicrecommendation/config/WebSocketConfig.java
package com.example.musicrecommendation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 프론트에서 SockJS('/ws')를 사용하므로 반드시 withSockJS() 필요
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new UserHandshakeInterceptor())
                .setHandshakeHandler(new UserPrincipalHandshakeHandler())
                .withSockJS();
        // 만약 네이티브 WebSocket도 같이 열고 싶다면(선택):
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new UserHandshakeInterceptor())
                .setHandshakeHandler(new UserPrincipalHandshakeHandler());
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

    /**
     * WebSocket 핸드셰이크 시 connectHeaders에서 userId 추출하여 Principal 설정
     */
    public static class UserHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response,
                                     WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            
            // URL 파라미터에서 userId 추출 시도
            String query = request.getURI().getQuery();
            log.info("[WS-HANDSHAKE] Query string: {}", query);
            
            if (query != null && query.contains("userId=")) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("userId=")) {
                        String userId = param.split("=")[1];
                        log.info("[WS-HANDSHAKE] URL에서 userId 추출: {}", userId);
                        attributes.put("userId", userId);
                        return true;
                    }
                }
            }
            
            log.warn("[WS-HANDSHAKE] userId를 찾을 수 없음 - 연결 허용하지만 Principal은 null");
            return true; // 연결 허용
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Exception exception) {
            if (exception != null) {
                log.error("[WS-HANDSHAKE] 핸드셰이크 오류:", exception);
            } else {
                log.info("[WS-HANDSHAKE] 핸드셰이크 완료");
            }
        }
    }

    /**
     * WebSocket Session에서 Principal을 생성하는 핸들러
     */
    public static class UserPrincipalHandshakeHandler extends DefaultHandshakeHandler {

        @Override
        protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
            // HandshakeInterceptor에서 설정한 userId 사용
            String userId = (String) attributes.get("userId");
            if (userId != null) {
                log.info("[WS-PRINCIPAL] userId {}로 Principal 생성", userId);
                return new UserPrincipal(userId);
            }
            
            log.warn("[WS-PRINCIPAL] userId가 없어 Principal을 null로 설정");
            return null; // Principal이 없으면 WebSocket 메시지에서 userId 추출 시도
        }
    }

    /**
     * 간단한 사용자 Principal 구현
     */
    public static class UserPrincipal implements Principal {
        private final String name;

        public UserPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "UserPrincipal{name='" + name + "'}";
        }
    }
}
