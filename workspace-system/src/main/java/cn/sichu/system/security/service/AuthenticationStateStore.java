package cn.sichu.system.security.service;

import cn.sichu.system.security.model.AccessTokenState;
import cn.sichu.system.security.model.SessionState;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public interface AuthenticationStateStore {

    void saveAccessToken(String tokenHash, AccessTokenState state);

    Optional<AccessTokenState> findAccessToken(String tokenHash);

    void saveSession(SessionState state);

    Optional<SessionState> findSession(String sessionCode);

    boolean touchSession(String sessionCode, Instant lastSeenAt);

    void revokeSession(String sessionCode, Instant revokedAt);

    default void revokeSessions(Collection<String> sessionCodes,
        Instant revokedAt) {
        sessionCodes.forEach(
            sessionCode -> revokeSession(sessionCode, revokedAt));
    }
}
