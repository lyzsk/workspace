package cn.sichu.system.security.service;

import cn.sichu.common.redis.cache.LoginSessionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Component
@RequiredArgsConstructor
public class AuthTokenStore {

    private final LoginSessionStore sessionStore;

    public void revokeSession(String sessionId) {
        if (sessionId != null) {
            sessionStore.delete(sessionId);
        }
    }

    public void revokeAllSessions(Long userId) {
        if (userId != null) {
            sessionStore.deleteByUserId(userId);
        }
    }
}
