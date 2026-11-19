package cn.sichu.system.security.filter;

import cn.sichu.system.security.token.OpaqueAuthenticationProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
@RequiredArgsConstructor
public class GatewayIntrospectionAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORITY = "INTERNAL_AUTH_INTROSPECTION";
    public static final String TOKEN_HEADER = "X-Workspace-Gateway-Token";
    private static final String PATH = "/api/internal/auth/introspect";

    private final OpaqueAuthenticationProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(TOKEN_HEADER);
        String expected = properties.getGatewayToken();
        if (token != null && expected != null && !expected.isEmpty()
            && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("gateway", null,
                    List.of(new SimpleGrantedAuthority(AUTHORITY))));
        }
        filterChain.doFilter(request, response);
    }
}
