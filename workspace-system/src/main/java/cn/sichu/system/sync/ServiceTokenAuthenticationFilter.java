package cn.sichu.system.sync;

import cn.sichu.system.security.service.ServiceTokenValidator;
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
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
@RequiredArgsConstructor
public class ServiceTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Workspace-Service-Token";
    public static final String AUTHORITY = "INTERNAL_SERVICE";

    private final ServiceTokenValidator tokenValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(HEADER);
        if (token != null) {
            try {
                tokenValidator.validate(token);
                SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("workspace-service", null,
                        List.of(new SimpleGrantedAuthority(AUTHORITY))));
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
