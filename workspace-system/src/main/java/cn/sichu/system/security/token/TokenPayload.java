package cn.sichu.system.security.token;

import java.time.Instant;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public record TokenPayload(Long userId, String username, Long tenantId, Long tokenVersion,
                           String tokenId, Instant issuedAt, Instant expiresAt) {
}