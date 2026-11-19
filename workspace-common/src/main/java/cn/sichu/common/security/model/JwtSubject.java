package cn.sichu.common.security.model;

import java.time.Instant;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public record JwtSubject(Long userId, String sessionId, Long tokenVersion,
                         Instant expiresAt) {
}
