package cn.sichu.system.security.service;

import cn.sichu.system.auth.model.vo.IntrospectionVO;
import cn.sichu.system.security.entity.SysRefreshToken;
import cn.sichu.system.security.entity.SysUserSession;
import cn.sichu.system.security.mapper.SysRefreshTokenMapper;
import cn.sichu.system.security.mapper.SysUserSessionMapper;
import cn.sichu.system.security.model.AccessTokenState;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.model.SessionState;
import cn.sichu.system.security.token.IssuedTokens;
import cn.sichu.system.security.token.OpaqueAuthenticationProperties;
import cn.sichu.system.security.token.OpaqueTokenGenerator;
import cn.sichu.system.security.token.SessionProperties;
import cn.sichu.system.security.token.TokenException;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
class OpaqueTokenServiceTest {

    private SysUserSessionMapper sessionMapper;
    private SysRefreshTokenMapper refreshTokenMapper;
    private SysUserMapper userMapper;
    private AuthTokenStore authTokenStore;
    private FakeAuthenticationStateStore stateStore;
    private OpaqueTokenGenerator tokenGenerator;
    private MutableClock clock;
    private OpaqueTokenService tokenService;
    private SysUser user;
    private SysUserSession session;
    private final Map<String, SysRefreshToken> refreshTokens = new HashMap<>();
    private final AtomicLong ids = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        sessionMapper = mock(SysUserSessionMapper.class);
        refreshTokenMapper = mock(SysRefreshTokenMapper.class);
        userMapper = mock(SysUserMapper.class);
        authTokenStore = mock(AuthTokenStore.class);
        stateStore = new FakeAuthenticationStateStore();
        tokenGenerator = new OpaqueTokenGenerator();
        clock = new MutableClock(Instant.parse("2026-11-19T00:00:00Z"));
        OpaqueAuthenticationProperties authenticationProperties = new OpaqueAuthenticationProperties();
        SessionProperties sessionProperties = new SessionProperties();
        tokenService = new OpaqueTokenService(sessionMapper, refreshTokenMapper, userMapper,
            authTokenStore, stateStore, mock(SessionActivityRecorder.class), tokenGenerator,
            authenticationProperties, sessionProperties, clock);
        user = new SysUser();
        user.setId(1L);
        user.setTenantId(0L);
        user.setUsername("admin");
        user.setStatus(1);
        user.setTokenVersion(0L);
        when(userMapper.selectById(1L)).thenReturn(user);
        doAnswer(invocation -> {
            SysUserSession inserted = invocation.getArgument(0);
            inserted.setId(ids.getAndIncrement());
            session = inserted;
            return 1;
        }).when(sessionMapper).insert(any(SysUserSession.class));
        when(sessionMapper.selectForUpdateById(anyLong())).thenAnswer(invocation -> session);
        when(sessionMapper.selectById(anyLong())).thenAnswer(invocation -> session);
        when(sessionMapper.updateById(any(SysUserSession.class))).thenReturn(1);
        doAnswer(invocation -> {
            SysRefreshToken inserted = invocation.getArgument(0);
            inserted.setId(ids.getAndIncrement());
            refreshTokens.put(inserted.getTokenHash(), inserted);
            return 1;
        }).when(refreshTokenMapper).insert(any(SysRefreshToken.class));
        when(refreshTokenMapper.selectByHash(anyString())).thenAnswer(
            invocation -> refreshTokens.get(invocation.getArgument(0)));
        when(refreshTokenMapper.selectForUpdateByHash(anyString())).thenAnswer(
            invocation -> refreshTokens.get(invocation.getArgument(0)));
        when(refreshTokenMapper.consume(anyLong(), any(), anyString())).thenAnswer(invocation -> {
            SysRefreshToken current = refreshTokens.values().stream()
                .filter(item -> item.getId().equals(invocation.getArgument(0))).findFirst().orElseThrow();
            if (current.getConsumedAt() != null || current.getRevokedAt() != null) {
                return 0;
            }
            current.setConsumedAt(invocation.getArgument(1));
            current.setReplacedByHash(invocation.getArgument(2));
            return 1;
        });
    }

    @Test
    void createsOpaqueTokensAndPersistsOnlyHashes() {
        IssuedTokens tokens = tokenService.createSession(loginUser());

        assertEquals(43, tokens.accessToken().length());
        assertEquals(43, tokens.refreshToken().length());
        assertFalse(tokens.accessToken().contains("."));
        assertNotEquals(tokens.accessToken(), tokens.refreshToken());
        assertEquals(session.getSessionCode(), session.getJti());
        assertNotNull(session.getRefreshFamily());
        SysRefreshToken persisted = refreshTokens.get(tokenGenerator.hash(tokens.refreshToken()));
        assertNotNull(persisted);
        assertNotEquals(tokens.refreshToken(), persisted.getTokenHash());
        assertTrue(stateStore.accessTokens.containsKey(tokenGenerator.hash(tokens.accessToken())));
        assertFalse(stateStore.accessTokens.keySet().stream().anyMatch(tokens.accessToken()::equals));
    }

    @Test
    void introspectionReturnsIdentityWithoutAuthorities() {
        IssuedTokens tokens = tokenService.createSession(loginUser());

        IntrospectionVO result = tokenService.introspect(tokens.accessToken());

        assertTrue(result.active());
        assertEquals(1L, result.userId());
        assertEquals("admin", result.username());
        assertEquals(0L, result.tenantId());
        assertEquals(session.getId(), result.sessionId());
        assertEquals(0L, result.tokenVersion());
        assertEquals(tokens.accessExpiresAt(), result.expiresAt());
    }

    @Test
    void introspectionRejectsExpiredAccessToken() {
        IssuedTokens tokens = tokenService.createSession(loginUser());
        clock.advance(Duration.ofMinutes(31));

        assertFalse(tokenService.introspect(tokens.accessToken()).active());
    }

    @Test
    void introspectionRejectsRevokedSession() {
        IssuedTokens tokens = tokenService.createSession(loginUser());
        stateStore.revokeSession(session.getSessionCode(), clock.instant());

        assertFalse(tokenService.introspect(tokens.accessToken()).active());
    }

    @Test
    void introspectionRejectsIdleSessionWithoutTouchingIt() {
        IssuedTokens tokens = tokenService.createSession(loginUser());
        SessionState before = stateStore.sessions.get(session.getSessionCode());
        clock.advance(Duration.ofMinutes(31));

        assertFalse(tokenService.introspect(tokens.accessToken()).active());
        assertEquals(before.lastSeenAt(), stateStore.sessions.get(session.getSessionCode()).lastSeenAt());
    }

    @Test
    void introspectionRejectsChangedUserTokenVersion() {
        IssuedTokens tokens = tokenService.createSession(loginUser());
        user.setTokenVersion(1L);

        assertFalse(tokenService.introspect(tokens.accessToken()).active());
    }

    @Test
    void refreshUsesRowLocksAndRotatesBothTokens() {
        IssuedTokens original = tokenService.createSession(loginUser());
        String oldHash = tokenGenerator.hash(original.refreshToken());

        IssuedTokens rotated = tokenService.refresh(original.refreshToken());

        assertNotEquals(original.accessToken(), rotated.accessToken());
        assertNotEquals(original.refreshToken(), rotated.refreshToken());
        assertNotNull(refreshTokens.get(oldHash).getConsumedAt());
        assertEquals(tokenGenerator.hash(rotated.refreshToken()),
            refreshTokens.get(oldHash).getReplacedByHash());
        verify(refreshTokenMapper).selectForUpdateByHash(oldHash);
        verify(sessionMapper).selectForUpdateById(session.getId());
    }

    @Test
    void refreshReuseRevokesWholeSessionAndExistingAccess() {
        IssuedTokens original = tokenService.createSession(loginUser());
        IssuedTokens rotated = tokenService.refresh(original.refreshToken());
        doAnswer(invocation -> {
            stateStore.revokeSession(session.getSessionCode(), clock.instant());
            return null;
        }).when(authTokenStore).revokeSessionById(session.getId());

        assertThrows(TokenException.class, () -> tokenService.refresh(original.refreshToken()));

        verify(authTokenStore).revokeSessionById(session.getId());
        assertFalse(tokenService.introspect(rotated.accessToken()).active());
    }

    @Test
    void logoutSupportsAccessTokenAndRefreshCookie() {
        IssuedTokens tokens = tokenService.createSession(loginUser());

        tokenService.logout(tokens.accessToken(), null);
        tokenService.logout(null, tokens.refreshToken());

        verify(authTokenStore, org.mockito.Mockito.times(2)).revokeSessionById(session.getId());
    }

    @Test
    void gatewayIdentityMustMatchPersistentAndRuntimeSession() {
        tokenService.createSession(loginUser());

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
            () -> tokenService.validateGatewaySession(1L, session.getId(), 0L));
        assertThrows(TokenException.class,
            () -> tokenService.validateGatewaySession(2L, session.getId(), 0L));
        assertThrows(TokenException.class,
            () -> tokenService.validateGatewaySession(1L, session.getId(), 1L));
    }

    @Test
    void redisFailureIsNotReportedAsInactiveToken() {
        IssuedTokens tokens = tokenService.createSession(loginUser());
        stateStore.failure = true;

        assertThrows(AuthenticationStateException.class,
            () -> tokenService.introspect(tokens.accessToken()));
    }

    private LoginUser loginUser() {
        return new LoginUser(user, List.of(), List.of());
    }

    private static class FakeAuthenticationStateStore implements AuthenticationStateStore {

        private final Map<String, AccessTokenState> accessTokens = new HashMap<>();
        private final Map<String, SessionState> sessions = new HashMap<>();
        private boolean failure;

        @Override
        public void saveAccessToken(String tokenHash, AccessTokenState state) {
            checkFailure();
            accessTokens.put(tokenHash, state);
        }

        @Override
        public Optional<AccessTokenState> findAccessToken(String tokenHash) {
            checkFailure();
            return Optional.ofNullable(accessTokens.get(tokenHash));
        }

        @Override
        public void saveSession(SessionState state) {
            checkFailure();
            sessions.put(state.sessionCode(), state);
        }

        @Override
        public Optional<SessionState> findSession(String sessionCode) {
            checkFailure();
            return Optional.ofNullable(sessions.get(sessionCode));
        }

        @Override
        public boolean touchSession(String sessionCode, Instant lastSeenAt) {
            checkFailure();
            SessionState state = sessions.get(sessionCode);
            if (state == null || state.revoked()) {
                return false;
            }
            sessions.put(sessionCode, new SessionState(state.sessionCode(), state.userId(),
                state.tokenVersion(), state.expiresAt(), lastSeenAt, false, null));
            return true;
        }

        @Override
        public void revokeSession(String sessionCode, Instant revokedAt) {
            checkFailure();
            SessionState state = sessions.get(sessionCode);
            if (state != null) {
                sessions.put(sessionCode, new SessionState(state.sessionCode(), state.userId(),
                    state.tokenVersion(), state.expiresAt(), state.lastSeenAt(), true, revokedAt));
            }
        }

        private void checkFailure() {
            if (failure) {
                throw new AuthenticationStateException("redis unavailable");
            }
        }
    }

    private static class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
