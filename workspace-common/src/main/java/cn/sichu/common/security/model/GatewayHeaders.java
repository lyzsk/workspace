package cn.sichu.common.security.model;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public final class GatewayHeaders {

    public static final String TOKEN = "X-Workspace-Gateway-Token";
    public static final String USER_ID = "X-Workspace-User-Id";
    public static final String SESSION_ID = "X-Workspace-Session-Id";
    public static final String TOKEN_VERSION = "X-Workspace-Token-Version";
    public static final String USERNAME = "X-Workspace-Username";
    public static final String TENANT_ID = "X-Workspace-Tenant-Id";

    private GatewayHeaders() {
    }
}
