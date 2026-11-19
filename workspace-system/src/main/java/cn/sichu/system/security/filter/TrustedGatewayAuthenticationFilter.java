package cn.sichu.system.security.filter;

import cn.sichu.common.redis.cache.CacheAccessException;
import cn.sichu.common.redis.cache.LoginSessionStore;
import cn.sichu.common.security.model.GatewayHeaders;
import cn.sichu.common.security.model.LoginSession;
import cn.sichu.common.security.model.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
@RequiredArgsConstructor
public class TrustedGatewayAuthenticationFilter extends OncePerRequestFilter {

    private final LoginSessionStore sessionStore;

    @Value("${workspace.auth.gateway-token:}")
    private String gatewayToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String actual = request.getHeader(GatewayHeaders.TOKEN);
        if (actual != null && gatewayToken != null && !gatewayToken.isBlank()
            && MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8),
                gatewayToken.getBytes(StandardCharsets.UTF_8))
            && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long userId = Long.valueOf(request.getHeader(GatewayHeaders.USER_ID));
                String sessionId = request.getHeader(GatewayHeaders.SESSION_ID);
                Long tokenVersion = Long.valueOf(request.getHeader(GatewayHeaders.TOKEN_VERSION));
                LoginSession session = sessionStore.find(sessionId).orElse(null);
                LocalDateTime now = LocalDateTime.now();
                if (session != null && !session.revoked()
                    && Objects.equals(session.loginUser().getUserId(), userId)
                    && Objects.equals(session.loginUser().getTokenVersion(), tokenVersion)
                    && now.isBefore(session.expiresAt())
                    && now.isBefore(session.lastSeenAt().plus(Duration.ofMinutes(30)))
                    && session.loginUser().isEnabled()) {
                    LoginUser loginUser = session.loginUser();
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null,
                            loginUser.getAuthorities());
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (NumberFormatException | CacheAccessException e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
