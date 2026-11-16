package cn.sichu.system.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "workspace.api-doc", name = "enabled", havingValue = "false",
    matchIfMissing = true)
public class ApiDocDisabledFilter extends OncePerRequestFilter {

    private static final String[] PATHS = {
        "/doc.html", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
        "/swagger-resources/**"
    };

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        for (String pattern : PATHS) {
            if (pathMatcher.match(pattern, path)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
