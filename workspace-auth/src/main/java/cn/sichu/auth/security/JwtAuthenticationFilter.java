package cn.sichu.auth.security;

import cn.sichu.auth.session.SessionProperties;
import cn.sichu.common.redis.cache.LoginSessionStore;
import cn.sichu.common.security.model.LoginSession;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.common.security.token.JwtTokenService;
import cn.sichu.common.security.token.TokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService tokenService;
    private final LoginSessionStore sessionStore;
    private final SessionProperties sessionProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")
            && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String sessionId = tokenService.parse(header.substring(7)).sessionId();
                LoginSession session = sessionStore.find(sessionId).orElse(null);
                LocalDateTime now = LocalDateTime.now();
                if (session != null && !session.revoked() && now.isBefore(session.expiresAt())
                    && now.isBefore(session.lastSeenAt().plus(sessionProperties.getIdleTimeout()))
                    && session.loginUser().isEnabled() && session.loginUser()
                    .isAccountNonLocked()) {
                    sessionStore.recordActivity(sessionId, now);
                    LoginUser loginUser = session.loginUser();
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null,
                            loginUser.getAuthorities());
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (TokenException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
