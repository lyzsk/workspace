package cn.sichu.system.security.websocket;

import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.service.AuthenticationStateException;
import cn.sichu.system.security.service.JwtAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeInterceptor;

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

    private final JwtAuthenticationService authenticationService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
        ServerHttpResponse response, WebSocketHandler wsHandler,
        Map<String, Object> attributes) throws Exception {
        String token =
            request.getHeaders().getValuesAsList(PROTOCOL_HEADER).stream()
                .flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank() && !"workspace".equals(value))
                .findFirst().orElse(null);
        if (token == null) {
            throw new HandshakeFailureException("缺少 WebSocket 登录凭证");
        }
        try {
            LoginUser loginUser = authenticationService.authenticate(token);
            attributes.put("loginUser", loginUser);
            response.getHeaders().set(PROTOCOL_HEADER, "workspace");
            return true;
        } catch (AuthenticationStateException e) {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
        ServerHttpResponse response, WebSocketHandler wsHandler,
        Exception exception) {
    }
}
