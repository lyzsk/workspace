package cn.sichu.system.security.service;

import cn.sichu.system.auth.model.vo.IntrospectionVO;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.token.TokenException;
import cn.sichu.system.user.entity.SysUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/17
 */
class JwtAuthenticationServiceTest {

    private OpaqueTokenService tokenService;
    private LoginUserDetailsService userDetailsService;
    private JwtAuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        tokenService = mock(OpaqueTokenService.class);
        userDetailsService = mock(LoginUserDetailsService.class);
        authenticationService = new JwtAuthenticationService(tokenService, userDetailsService);
    }

    @Test
    void authenticatesPrincipalByUserId() {
        when(tokenService.introspect("token")).thenReturn(
            new IntrospectionVO(true, 42L, "old-name", 0L, 9L, 3L, Instant.now()));
        LoginUser loginUser = loginUser(3L);
        when(userDetailsService.loadUserById(42L)).thenReturn(loginUser);

        LoginUser result = authenticationService.authenticate("token");

        assertSame(loginUser, result);
        verify(userDetailsService).loadUserById(42L);
    }

    @Test
    void rejectsInactiveIntrospection() {
        when(tokenService.introspect("token")).thenReturn(IntrospectionVO.inactive());

        assertThrows(TokenException.class, () -> authenticationService.authenticate("token"));

        verifyNoInteractions(userDetailsService);
    }

    @Test
    void rejectsChangedTokenVersion() {
        when(tokenService.introspect("token")).thenReturn(
            new IntrospectionVO(true, 42L, "user", 0L, 9L, 2L, Instant.now()));
        when(userDetailsService.loadUserById(42L)).thenReturn(loginUser(3L));

        assertThrows(TokenException.class, () -> authenticationService.authenticate("token"));
    }

    private LoginUser loginUser(Long tokenVersion) {
        SysUser user = new SysUser();
        user.setId(42L);
        user.setUsername("user");
        user.setTenantId(0L);
        user.setTokenVersion(tokenVersion);
        user.setStatus(1);
        return new LoginUser(user, List.of(), List.of());
    }
}
