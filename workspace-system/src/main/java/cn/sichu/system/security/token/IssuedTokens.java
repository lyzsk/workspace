package cn.sichu.system.security.token;

import java.time.Instant;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public record IssuedTokens(String accessToken, String refreshToken, Long userId,
                           Instant accessExpiresAt, Instant sessionExpiresAt) {
}
