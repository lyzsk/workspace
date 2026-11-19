package cn.sichu.system.security.websocket;

import cn.sichu.common.redis.cache.LoginSessionStore;
import cn.sichu.common.security.model.LoginSession;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.common.security.token.JwtTokenService;
import cn.sichu.common.security.token.TokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final String PROTOCOL_HEADER = "Sec-WebSocket-Protocol";

    private final JwtTokenService tokenService;
    private final LoginSessionStore sessionStore;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
        WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = request.getHeaders().getValuesAsList(PROTOCOL_HEADER).stream()
            .flatMap(value -> Arrays.stream(value.split(",")))
            .map(String::trim).filter(value -> !value.isBlank() && !"workspace".equals(value))
            .findFirst().orElse(null);
        if (token == null) {
            throw new HandshakeFailureException("缺少 WebSocket 登录凭证");
        }
        try {
            String sessionId = tokenService.parse(token).sessionId();
            LoginSession session = sessionStore.find(sessionId)
                .orElseThrow(() -> new TokenException("登录会话不存在"));
            LoginUser loginUser = session.loginUser();
            LocalDateTime now = LocalDateTime.now();
            if (session.revoked() || now.isAfter(session.expiresAt())
                || now.isAfter(session.lastSeenAt().plus(java.time.Duration.ofMinutes(30)))
                || !loginUser.isEnabled()) {
                throw new TokenException("登录会话无效");
            }
            sessionStore.recordActivity(sessionId, now);
            attributes.put("loginUser", loginUser);
            response.getHeaders().set(PROTOCOL_HEADER, "workspace");
            return true;
        } catch (TokenException exception) {
            throw new HandshakeFailureException("WebSocket 登录凭证无效", exception);
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
        WebSocketHandler wsHandler, Exception exception) {
    }
}
