package cn.sichu.system.auth.model.vo;

import java.time.Instant;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public record IntrospectionVO(
    boolean active,
    Long userId,
    String username,
    Long tenantId,
    Long sessionId,
    Long tokenVersion,
    Instant expiresAt
) {

    public static IntrospectionVO inactive() {
        return new IntrospectionVO(false, null, null, null, null, null, null);
    }
}
