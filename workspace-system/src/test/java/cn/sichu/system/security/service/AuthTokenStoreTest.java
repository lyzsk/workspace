package cn.sichu.system.security.service;

import cn.sichu.system.security.entity.SysUserSession;
import cn.sichu.system.security.mapper.SysRefreshTokenMapper;
import cn.sichu.system.security.mapper.SysTokenBlacklistMapper;
import cn.sichu.system.security.mapper.SysUserSessionMapper;
import cn.sichu.system.security.model.AccessTokenState;
import cn.sichu.system.security.model.SessionState;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
class AuthTokenStoreTest {

    private SysUserSessionMapper sessionMapper;
    private SysRefreshTokenMapper refreshTokenMapper;
    private RecordingStateStore stateStore;
    private AuthTokenStore authTokenStore;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            SysUserSession.class);
    }

    @BeforeEach
    void setUp() {
        sessionMapper = mock(SysUserSessionMapper.class);
        refreshTokenMapper = mock(SysRefreshTokenMapper.class);
        stateStore = new RecordingStateStore();
        authTokenStore = new AuthTokenStore(sessionMapper, mock(SysTokenBlacklistMapper.class),
            refreshTokenMapper, mock(SessionActivityRecorder.class), stateStore);
    }

    @Test
    void revokeAllSessionsLocksAndRevokesClusterStateAndRefreshMetadata() {
        SysUserSession first = session(11L, "session-a");
        SysUserSession second = session(12L, "session-b");
        when(sessionMapper.selectForUpdateByUserId(1L)).thenReturn(List.of(first, second));

        authTokenStore.revokeAllSessions(1L);

        verify(sessionMapper).selectForUpdateByUserId(1L);
        verify(sessionMapper).update(any(), any());
        verify(refreshTokenMapper).revokeBySessionId(org.mockito.ArgumentMatchers.eq(11L), any());
        verify(refreshTokenMapper).revokeBySessionId(org.mockito.ArgumentMatchers.eq(12L), any());
        assertEquals(List.of("session-a", "session-b"), stateStore.revoked);
    }

    @Test
    void revokeSessionByIdUsesRowLock() {
        when(sessionMapper.selectForUpdateById(11L)).thenReturn(session(11L, "session-a"));

        authTokenStore.revokeSessionById(11L);

        verify(sessionMapper).selectForUpdateById(11L);
        assertEquals(List.of("session-a"), stateStore.revoked);
    }

    private SysUserSession session(Long id, String sessionCode) {
        SysUserSession session = new SysUserSession();
        session.setId(id);
        session.setUserId(1L);
        session.setJti(sessionCode);
        session.setSessionCode(sessionCode);
        session.setRevoked(0);
        return session;
    }

    private static class RecordingStateStore implements AuthenticationStateStore {

        private final List<String> revoked = new ArrayList<>();

        @Override
        public void saveAccessToken(String tokenHash, AccessTokenState state) {
        }

        @Override
        public Optional<AccessTokenState> findAccessToken(String tokenHash) {
            return Optional.empty();
        }

        @Override
        public void saveSession(SessionState state) {
        }

        @Override
        public Optional<SessionState> findSession(String sessionCode) {
            return Optional.empty();
        }

        @Override
        public boolean touchSession(String sessionCode, Instant lastSeenAt) {
            return false;
        }

        @Override
        public void revokeSession(String sessionCode, Instant revokedAt) {
            revoked.add(sessionCode);
        }
    }
}
