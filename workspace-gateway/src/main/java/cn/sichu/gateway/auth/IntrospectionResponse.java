package cn.sichu.gateway.auth;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public record IntrospectionResponse(Boolean active, String userId,
                                    String username, String tenantId,
                                    String sessionId, String tokenVersion,
                                    String expiresAt) {
}
