package cn.sichu.system.security.model;

import java.time.Instant;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public record AccessTokenState(Long userId, String username, Long tenantId,
                               Long sessionId, String sessionCode,
                               Long tokenVersion, Instant issuedAt,
                               Instant expiresAt) {
}
