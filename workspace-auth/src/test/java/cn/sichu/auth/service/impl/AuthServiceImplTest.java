package cn.sichu.auth.service.impl;

import cn.sichu.auth.model.form.LoginForm;
import cn.sichu.auth.service.IRemoteLoginLogService;
import cn.sichu.auth.service.IRemoteUserService;
import cn.sichu.auth.service.RemoteLoginUserDetailsService;
import cn.sichu.auth.service.ServiceTokenProvider;
import cn.sichu.auth.session.LoginSessionLock;
import cn.sichu.auth.session.SessionProperties;
import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.license.LicenseState;
import cn.sichu.common.license.LicenseStateStore;
import cn.sichu.common.license.LicenseStatus;
import cn.sichu.common.redis.cache.LoginSessionStore;
import cn.sichu.common.security.model.LoginSession;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.common.security.token.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
class AuthServiceImplTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtTokenService tokenService = mock(JwtTokenService.class);
    private final LoginSessionStore sessionStore = mock(LoginSessionStore.class);
    private final IRemoteUserService remoteUserService = mock(IRemoteUserService.class);
    private final IRemoteLoginLogService loginLogService = mock(IRemoteLoginLogService.class);
    private final RemoteLoginUserDetailsService userDetailsService = mock(RemoteLoginUserDetailsService.class);
    private final ServiceTokenProvider tokenProvider = mock(ServiceTokenProvider.class);
    private final LoginSessionLock loginSessionLock = mock(LoginSessionLock.class);
    private final LicenseStateStore licenseStateStore = mock(LicenseStateStore.class);
    private final SessionProperties sessionProperties = new SessionProperties();
    private final AuthServiceImpl service = new AuthServiceImpl(authenticationManager, tokenService,
        sessionStore, remoteUserService, loginLogService, userDetailsService, tokenProvider,
        sessionProperties, loginSessionLock, licenseStateStore);

    @org.junit.jupiter.api.BeforeEach
    void allowLicense() {
        LicenseState state = new LicenseState();
        state.setStatus(LicenseStatus.VALID);
        when(licenseStateStore.find()).thenReturn(Optional.of(state));
    }

    @Test
    void revokesPreviousSessionBeforeIssuingNewToken() {
        LoginUser user = mock(LoginUser.class);
        when(user.getUserId()).thenReturn(1L);
        when(user.getUsername()).thenReturn("admin");
        when(authenticationManager.authenticate(any()))
            .thenReturn(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        when(tokenService.createToken(eq(user), anyString())).thenReturn("jwt");
        when(loginSessionLock.withLock(eq(1L), any())).thenAnswer(invocation ->
            ((Supplier<?>) invocation.getArgument(1)).get());

        service.login(new LoginForm("admin", "password"));

        InOrder order = inOrder(sessionStore, tokenService);
        order.verify(sessionStore).deleteByUserId(1L);
        order.verify(sessionStore).save(any(LoginSession.class));
        order.verify(tokenService).createToken(eq(user), anyString());
    }

    @Test
    void canAllowConcurrentSessionsWhenConfigured() {
        sessionProperties.setSingleSession(false);
        LoginUser user = mock(LoginUser.class);
        when(user.getUserId()).thenReturn(1L);
        when(user.getUsername()).thenReturn("admin");
        when(authenticationManager.authenticate(any()))
            .thenReturn(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        when(tokenService.createToken(eq(user), anyString())).thenReturn("jwt");
        when(loginSessionLock.withLock(eq(1L), any())).thenAnswer(invocation ->
            ((Supplier<?>) invocation.getArgument(1)).get());

        service.login(new LoginForm("admin", "password"));

        InOrder order = inOrder(sessionStore, tokenService);
        order.verify(sessionStore).save(any(LoginSession.class));
        order.verify(tokenService).createToken(eq(user), anyString());
    }

    @Test
    void blocksNonEmergencyUserWhenLicenseStateMissing() {
        when(licenseStateStore.find()).thenReturn(Optional.empty());
        LoginUser user = mock(LoginUser.class);
        when(user.getRoles()).thenReturn(Set.of("TEST"));
        when(authenticationManager.authenticate(any()))
            .thenReturn(new UsernamePasswordAuthenticationToken(user, null, List.of()));

        assertThrows(BusinessException.class,
            () -> service.login(new LoginForm("test", "password")));
    }

    @Test
    void allowsEmergencyAdminWhenLicenseStateMissing() {
        when(licenseStateStore.find()).thenReturn(Optional.empty());
        LoginUser user = mock(LoginUser.class);
        when(user.getUserId()).thenReturn(1L);
        when(user.getUsername()).thenReturn("ops");
        when(user.getRoles()).thenReturn(Set.of("SUPER_ADMIN"));
        when(authenticationManager.authenticate(any()))
            .thenReturn(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        when(tokenService.createToken(eq(user), anyString())).thenReturn("jwt");
        when(loginSessionLock.withLock(eq(1L), any())).thenAnswer(invocation ->
            ((Supplier<?>) invocation.getArgument(1)).get());

        service.login(new LoginForm("ops", "password"));
    }
}
