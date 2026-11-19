package cn.sichu.system.security.model;

import java.util.Set;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public record DataScopeFilter(boolean all, Set<Long> deptIds, boolean selfOnly,
                              Long userId) {
}
