package cn.sichu.system.auth.model;

import cn.sichu.system.auth.model.vo.LoginVO;

import java.time.Instant;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public record LoginAuthentication(
    LoginVO login,
    String refreshToken,
    Instant sessionExpiresAt
) {
}
