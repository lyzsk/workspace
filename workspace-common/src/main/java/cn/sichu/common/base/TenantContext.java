package cn.sichu.common.base;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public class TenantContext {

    public static final long DEFAULT_TENANT_ID = 0L;

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Long getTenantId() {
        Long tenantId = HOLDER.get();
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    public static void set(Long tenantId) {
        HOLDER.set(tenantId);
    }

    public static void clear() {
        HOLDER.remove();
    }
}
