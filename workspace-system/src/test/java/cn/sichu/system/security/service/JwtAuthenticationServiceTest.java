package cn.sichu.system.security.service;

import cn.sichu.system.security.entity.SysUserSession;
import cn.sichu.system.security.mapper.SysUserSessionMapper;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.token.JwtTokenManager;
import cn.sichu.system.security.token.SessionProperties;
import cn.sichu.system.security.token.TokenException;
import cn.sichu.system.security.token.TokenPayload;
import cn.sichu.system.user.entity.SysUser;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    private JwtTokenManager tokenManager;
    private UserDetailsService userDetailsService;
    private AuthTokenStore authTokenStore;
    private SessionActivityRecorder activityRecorder;
    private SysUserSessionMapper sessionMapper;
    private JwtAuthenticationService authenticationService;
    private TokenPayload payload;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            SysUserSession.class);
    }

    @BeforeEach
    void setUp() {
        tokenManager = mock(JwtTokenManager.class);
        userDetailsService = mock(UserDetailsService.class);
        authTokenStore = mock(AuthTokenStore.class);
        activityRecorder = mock(SessionActivityRecorder.class);
        sessionMapper = mock(SysUserSessionMapper.class);
        SessionProperties properties = new SessionProperties();
        properties.setIdleTimeout(Duration.ofMinutes(30));
        authenticationService = new JwtAuthenticationService(tokenManager, userDetailsService,
            authTokenStore, activityRecorder, sessionMapper, properties);
        payload = new TokenPayload(1L, "abc", 0L, 1L, "jti-1", Instant.now(),
            Instant.now().plus(Duration.ofHours(12)));
        when(tokenManager.parseAccessToken("token")).thenReturn(payload);
    }

    @Test
    void authenticatesActiveSessionAndRecordsActivity() {
        when(sessionMapper.selectOne(any())).thenReturn(session(LocalDateTime.now()));
        LoginUser loginUser = loginUser();
        when(userDetailsService.loadUserByUsername("abc")).thenReturn(loginUser);

        LoginUser result = authenticationService.authenticate("token");

        assertSame(loginUser, result);
        verify(activityRecorder).record("jti-1");
    }

    @Test
    void rejectsMissingSession() {
        when(sessionMapper.selectOne(any())).thenReturn(null);

        assertThrows(TokenException.class, () -> authenticationService.authenticate("token"));

        verifyNoInteractions(userDetailsService, authTokenStore, activityRecorder);
    }

    @Test
    void rejectsIdleSessionWithoutRefreshingIt() {
        when(sessionMapper.selectOne(any()))
            .thenReturn(session(LocalDateTime.now().minusMinutes(31)));

        assertThrows(TokenException.class, () -> authenticationService.authenticate("token"));

        verifyNoInteractions(userDetailsService, authTokenStore, activityRecorder);
    }

    @Test
    void rejectsRevokedSession() {
        SysUserSession session = session(LocalDateTime.now());
        session.setRevoked(1);
        when(sessionMapper.selectOne(any())).thenReturn(session);

        assertThrows(TokenException.class, () -> authenticationService.authenticate("token"));

        verifyNoInteractions(userDetailsService, authTokenStore, activityRecorder);
    }

    @Test
    void rejectsAbsolutelyExpiredSession() {
        SysUserSession session = session(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(sessionMapper.selectOne(any())).thenReturn(session);

        assertThrows(TokenException.class, () -> authenticationService.authenticate("token"));

        verifyNoInteractions(userDetailsService, authTokenStore, activityRecorder);
    }

    private SysUserSession session(LocalDateTime lastSeenAt) {
        SysUserSession session = new SysUserSession();
        session.setUserId(1L);
        session.setJti("jti-1");
        session.setLastSeenAt(lastSeenAt);
        session.setExpiresAt(LocalDateTime.now().plusHours(12));
        session.setRevoked(0);
        return session;
    }

    private LoginUser loginUser() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("abc");
        user.setTenantId(0L);
        user.setTokenVersion(1L);
        user.setStatus(1);
        return new LoginUser(user, List.of(), List.of());
    }
}
