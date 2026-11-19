package cn.sichu.gateway.auth;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthenticationWebFilter implements WebFilter {

    public static final String GATEWAY_TOKEN_HEADER = "X-Workspace-Gateway-Token";
    public static final String USER_ID_HEADER = "X-Workspace-User-Id";
    public static final String USERNAME_HEADER = "X-Workspace-Username";
    public static final String TENANT_ID_HEADER = "X-Workspace-Tenant-Id";
    public static final String SESSION_ID_HEADER = "X-Workspace-Session-Id";
    public static final String TOKEN_VERSION_HEADER = "X-Workspace-Token-Version";
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/auth/login", "/api/auth/refresh", "/api/auth/logout", "/actuator/health", "/error");
    private static final List<String> IDENTITY_HEADERS = List.of(GATEWAY_TOKEN_HEADER,
        USER_ID_HEADER, USERNAME_HEADER, TENANT_ID_HEADER, SESSION_ID_HEADER, TOKEN_VERSION_HEADER);

    private final GatewayTokenVerifier verifier;
    private final GatewayAuthenticationProperties properties;
    private final GatewayErrorResponseWriter errorWriter;

    public AuthenticationWebFilter(GatewayTokenVerifier verifier,
        GatewayAuthenticationProperties properties, GatewayErrorResponseWriter errorWriter) {
        this.verifier = verifier;
        this.properties = properties;
        this.errorWriter = errorWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        ServerWebExchange cleaned = stripIdentityHeaders(exchange);
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod()) || isPublic(path)
            || path.startsWith("/api/auth/")) {
            return chain.filter(cleaned);
        }
        if (path.startsWith("/api/internal/")) {
            return errorWriter.write(exchange, HttpStatus.FORBIDDEN, "没有权限");
        }
        String token = extractToken(exchange);
        if (token == null || token.isBlank()) {
            return errorWriter.write(exchange, HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
        }
        return verifier.verify(token)
            .flatMap(identity -> {
                ServerWebExchange authenticated = cleaned.mutate().request(builder -> {
                    builder.headers(headers -> headers.remove(HttpHeaders.AUTHORIZATION));
                    builder.header(GATEWAY_TOKEN_HEADER, properties.getGatewayToken());
                    builder.header(USER_ID_HEADER, String.valueOf(identity.userId()));
                    builder.header(SESSION_ID_HEADER, identity.sessionId());
                    if (identity.username() != null) {
                        builder.header(USERNAME_HEADER, identity.username());
                    }
                    if (identity.tenantId() != null) {
                        builder.header(TENANT_ID_HEADER, identity.tenantId());
                    }
                    if (identity.tokenVersion() != null) {
                        builder.header(TOKEN_VERSION_HEADER, String.valueOf(identity.tokenVersion()));
                    }
                }).build();
                return chain.filter(authenticated);
            })
            .switchIfEmpty(Mono.defer(() -> errorWriter.write(exchange,
                HttpStatus.UNAUTHORIZED, "未登录或登录已过期")));
    }

    private ServerWebExchange stripIdentityHeaders(ServerWebExchange exchange) {
        return exchange.mutate().request(builder -> builder.headers(headers -> {
            IDENTITY_HEADERS.forEach(headers::remove);
            headers.remove("X-Workspace-Auth-Source");
        })).build();
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.contains(path) || path.startsWith("/actuator/health/");
    }

    private String extractToken(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        if (!exchange.getRequest().getPath().value().startsWith("/ws/")) {
            return null;
        }
        return exchange.getRequest().getHeaders().getValuesAsList("Sec-WebSocket-Protocol").stream()
            .flatMap(value -> java.util.Arrays.stream(value.split(",")))
            .map(String::trim).filter(value -> !value.isBlank() && !"workspace".equals(value))
            .findFirst().orElse(null);
    }
}
