package cn.sichu.system.config;

import cn.sichu.system.notice.websocket.NotificationWebSocketHandler;
import cn.sichu.system.security.websocket.JwtHandshakeInterceptor;
import cn.sichu.tool.document.websocket.DocumentAccessHandshakeInterceptor;
import cn.sichu.tool.document.websocket.DocumentWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationHandler;
    private final DocumentWebSocketHandler documentHandler;
    private final DocumentAccessHandshakeInterceptor documentAccessInterceptor;
    private final JwtHandshakeInterceptor handshakeInterceptor;
    private final CorsProperties corsProperties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationHandler, "/ws/notifications")
            .addInterceptors(handshakeInterceptor)
            .setAllowedOrigins(corsProperties.getAllowedOrigins().toArray(String[]::new));
        registry.addHandler(documentHandler, "/ws/documents/{id}")
            .addInterceptors(handshakeInterceptor, documentAccessInterceptor)
            .setAllowedOrigins(corsProperties.getAllowedOrigins().toArray(String[]::new));
    }
}
