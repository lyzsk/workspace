package cn.sichu.system.sync;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/17
 */
@Component
@RequiredArgsConstructor
public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORITY = "INTERNAL_USER_SYNC";
    private static final String USERS_PATH_PREFIX = "/api/internal/users/";

    private final InternalUserSyncProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith(USERS_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, "Basic ", 0, 6)) {
            authenticate(header.substring(6));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String encoded) {
        try {
            String credentials = new String(Base64.getDecoder().decode(encoded),
                StandardCharsets.UTF_8);
            int separator = credentials.indexOf(':');
            if (separator <= 0) {
                return;
            }
            String appCode = credentials.substring(0, separator);
            String token = credentials.substring(separator + 1);
            boolean matches = properties.getClients().stream()
                .anyMatch(client -> appCode.equals(client.getAppCode())
                    && client.getToken() != null
                    && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                        client.getToken().getBytes(StandardCharsets.UTF_8)));
            if (matches) {
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(appCode, null,
                        List.of(new SimpleGrantedAuthority(AUTHORITY)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }
}
