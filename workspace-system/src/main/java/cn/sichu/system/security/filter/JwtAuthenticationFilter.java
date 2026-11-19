package cn.sichu.system.security.filter;

import cn.sichu.system.security.handler.ApiErrorWriter;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.service.AuthenticationStateException;
import cn.sichu.system.security.service.JwtAuthenticationService;
import cn.sichu.system.security.token.TokenException;
import cn.sichu.common.result.ResultCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtAuthenticationService authenticationService;
    private final ApiErrorWriter errorWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)
            && request.getHeader(GatewayIntrospectionAuthenticationFilter.TOKEN_HEADER) == null
            && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                LoginUser loginUser = authenticationService.authenticate(token);
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null,
                        loginUser.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (TokenException | UsernameNotFoundException e) {
                log.debug("不透明凭证认证失败: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            } catch (AuthenticationStateException e) {
                SecurityContextHolder.clearContext();
                errorWriter.write(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    ResultCode.FAILED, "认证状态服务不可用");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
