package kakao.login.config;

import jakarta.servlet.http.HttpServletRequest;
import kakao.login.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtProvider jwtProvider;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000", "http://localhost:4040") // Add both origins
                .addInterceptors(new HttpSessionHandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        if (request instanceof ServletServerHttpRequest) {
                            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
                            String token = extractToken(servletRequest);

                            if (token != null) {
                                try {
                                    // JwtProvider로 토큰 검증
                                    String userId = jwtProvider.validate(token);
                                    if (userId != null) {
                                        // SecurityContext에 인증 정보 설정
                                        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                                        securityContext.setAuthentication(
                                                new UsernamePasswordAuthenticationToken(userId, null,
                                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                                        );
                                        SecurityContextHolder.setContext(securityContext);

                                        // WebSocket 세션 속성에 userId 저장
                                        attributes.put("userId", userId);
                                        return true;
                                    }
                                } catch (Exception e) {
                                    System.err.println("Token validation error: " + e.getMessage());
                                    response.setStatusCode(HttpStatus.FORBIDDEN); // Set 403
                                    return false; // Deny handshake
                                }
                                response.setStatusCode(HttpStatus.UNAUTHORIZED); // Set 401
                                return false;
                            }
                            // 인증 실패 시에도 연결은 허용 (필요에 따라 변경 가능)
                            return true;
                        }
                        return true;
                    }
                })
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null) {
                    // Extract JWT token from headers
                    String token = extractTokenFromHeaders(accessor);

                    if (token != null) {
                        try {
                            // Validate token
                            String userId = jwtProvider.validate(token);

                            if (userId != null) {
                                // Create authentication object
                                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                        userId,
                                        null,
                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                                );

                                // Set authentication in message headers
                                accessor.setUser(authentication);
                            }
                        } catch (Exception e) {
                            // 로그 기록 (선택사항)
                            System.err.println("Error validating token: " + e.getMessage());
                        }
                    }
                }

                return message;
            }
        });
    }

    private String extractToken(HttpServletRequest request) {
        String token = request.getParameter("accessToken");
        if (token != null) return token;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }


    private String extractTokenFromHeaders(MessageHeaderAccessor accessor) {
        // STOMP 헤더에서 토큰 추출
        Object nativeHeaders = accessor.getHeader("nativeHeaders");
        if (nativeHeaders instanceof Map) {
            @SuppressWarnings("unchecked")
            List<String> authHeaders = (List<String>) ((Map<String, List<String>>) nativeHeaders).get("Authorization");

            if (authHeaders != null && !authHeaders.isEmpty()) {
                String authHeader = authHeaders.get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    return authHeader.substring(7);
                }
            }
        }
        return null;
    }
}