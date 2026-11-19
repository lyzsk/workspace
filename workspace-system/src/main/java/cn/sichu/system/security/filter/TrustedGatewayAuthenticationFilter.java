package cn.sichu.system.security.filter;

import cn.sichu.common.result.ResultCode;
import cn.sichu.system.security.handler.ApiErrorWriter;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.service.AuthenticationStateException;
import cn.sichu.system.security.service.LoginUserDetailsService;
import cn.sichu.system.security.service.OpaqueTokenService;
import cn.sichu.system.security.token.OpaqueAuthenticationProperties;
import cn.sichu.system.security.token.TokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
@RequiredArgsConstructor
public class TrustedGatewayAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-Workspace-User-Id";
    public static final String SESSION_ID_HEADER = "X-Workspace-Session-Id";
    public static final String TOKEN_VERSION_HEADER = "X-Workspace-Token-Version";
    private static final String INTROSPECTION_PATH = "/api/internal/auth/introspect";

    private final OpaqueAuthenticationProperties properties;
    private final OpaqueTokenService tokenService;
    private final LoginUserDetailsService userDetailsService;
    private final ApiErrorWriter errorWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return INTROSPECTION_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String gatewayToken = request.getHeader(GatewayIntrospectionAuthenticationFilter.TOKEN_HEADER);
        if (gatewayToken == null) {
            filterChain.doFilter(request, response);
            return;
        }
        String expected = properties.getGatewayToken();
        if (expected == null || expected.isEmpty() || !MessageDigest.isEqual(
            gatewayToken.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            Long userId = parseLong(request.getHeader(USER_ID_HEADER));
            Long sessionId = parseLong(request.getHeader(SESSION_ID_HEADER));
            Long tokenVersion = parseLong(request.getHeader(TOKEN_VERSION_HEADER));
            tokenService.validateGatewaySession(userId, sessionId, tokenVersion);
            LoginUser loginUser = userDetailsService.loadUserById(userId);
            if (!loginUser.isEnabled() || !loginUser.isAccountNonLocked()
                || !Objects.equals(tokenVersion, loginUser.getTokenVersion())) {
                throw new TokenException("Gateway 用户状态已变化");
            }
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (NumberFormatException | TokenException | UsernameNotFoundException e) {
            SecurityContextHolder.clearContext();
        } catch (AuthenticationStateException e) {
            SecurityContextHolder.clearContext();
            errorWriter.write(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                ResultCode.FAILED, "认证状态服务不可用");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Long parseLong(String value) {
        if (value == null) {
            throw new NumberFormatException("missing identity header");
        }
        return Long.valueOf(value);
    }
}
