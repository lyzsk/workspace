package cn.sichu.system.security.model;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public final class DataScopeContext {

    private static final ThreadLocal<DataScopeFilter> CURRENT = new ThreadLocal<>();

    private DataScopeContext() {
    }

    public static void set(DataScopeFilter filter) {
        CURRENT.set(filter);
    }

    public static DataScopeFilter get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
