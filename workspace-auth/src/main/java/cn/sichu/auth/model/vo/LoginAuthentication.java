package cn.sichu.auth.model.vo;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public record LoginAuthentication(LoginVO login, String sessionId, LocalDateTime sessionExpiresAt) {
}
