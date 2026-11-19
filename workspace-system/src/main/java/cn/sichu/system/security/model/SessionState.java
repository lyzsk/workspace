package cn.sichu.system.security.model;

import java.time.Instant;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public record SessionState(String sessionCode, Long userId, Long tokenVersion,
                           Instant expiresAt, Instant lastSeenAt,
                           boolean revoked, Instant revokedAt) {
}
