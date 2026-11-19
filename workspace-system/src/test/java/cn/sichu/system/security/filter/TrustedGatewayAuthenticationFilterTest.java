package cn.sichu.system.security.filter;

import cn.sichu.system.security.handler.ApiErrorWriter;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.service.LoginUserDetailsService;
import cn.sichu.system.security.service.OpaqueTokenService;
import cn.sichu.system.security.token.OpaqueAuthenticationProperties;
import cn.sichu.system.security.token.TokenException;
import cn.sichu.system.user.entity.SysUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
class TrustedGatewayAuthenticationFilterTest {

    private OpaqueTokenService tokenService;
    private LoginUserDetailsService userDetailsService;
    private TrustedGatewayAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        OpaqueAuthenticationProperties properties = new OpaqueAuthenticationProperties();
        properties.setGatewayToken("gateway-secret");
        tokenService = mock(OpaqueTokenService.class);
        userDetailsService = mock(LoginUserDetailsService.class);
        filter = new TrustedGatewayAuthenticationFilter(properties, tokenService, userDetailsService,
            new ApiErrorWriter(new ObjectMapper()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loadsPrincipalByUserIdAfterSessionBinding() throws Exception {
        MockHttpServletRequest request = request();
        LoginUser loginUser = loginUser();
        when(userDetailsService.loadUserById(42L)).thenReturn(loginUser);

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertEquals(42L, ((LoginUser) authentication.getPrincipal()).getUserId());
        });

        verify(tokenService).validateGatewaySession(42L, 9L, 3L);
        verify(userDetailsService).loadUserById(42L);
    }

    @Test
    void rejectsWrongGatewayCredential() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.setServletPath("/api/auth/me");
        request.addHeader(GatewayIntrospectionAuthenticationFilter.TOKEN_HEADER, "wrong");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
            assertNull(SecurityContextHolder.getContext().getAuthentication()));

        verifyNoInteractions(tokenService, userDetailsService);
    }

    @Test
    void rejectsSessionOrTokenVersionMismatch() throws Exception {
        MockHttpServletRequest request = request();
        doThrow(new TokenException("mismatch")).when(tokenService)
            .validateGatewaySession(42L, 9L, 3L);

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
            assertNull(SecurityContextHolder.getContext().getAuthentication()));

        verifyNoInteractions(userDetailsService);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.setServletPath("/api/auth/me");
        request.addHeader(GatewayIntrospectionAuthenticationFilter.TOKEN_HEADER, "gateway-secret");
        request.addHeader(TrustedGatewayAuthenticationFilter.USER_ID_HEADER, "42");
        request.addHeader(TrustedGatewayAuthenticationFilter.SESSION_ID_HEADER, "9");
        request.addHeader(TrustedGatewayAuthenticationFilter.TOKEN_VERSION_HEADER, "3");
        return request;
    }

    private LoginUser loginUser() {
        SysUser user = new SysUser();
        user.setId(42L);
        user.setUsername("current-name");
        user.setStatus(1);
        user.setTokenVersion(3L);
        return new LoginUser(user, List.of(), List.of());
    }
}
