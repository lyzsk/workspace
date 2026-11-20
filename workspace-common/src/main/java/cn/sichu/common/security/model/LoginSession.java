package cn.sichu.common.security.model;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public record LoginSession(String sessionId, LoginUser loginUser,
                           LocalDateTime loginTime, LocalDateTime lastSeenAt,
                           LocalDateTime expiresAt, boolean revoked,
                           LocalDateTime revokedAt, String ip, String userAgent) {
}
