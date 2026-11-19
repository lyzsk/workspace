package cn.sichu.common.core.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.Map;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public final class QuerySortUtils {

    private QuerySortUtils() {
    }

    public static <T> boolean apply(LambdaQueryWrapper<T> wrapper,
        String sortBy, String order, Map<String, SFunction<T, ?>> allowed) {
        if (sortBy == null || sortBy.isBlank()) {
            return false;
        }
        SFunction<T, ?> column = allowed.get(sortBy);
        if (column == null) {
            return false;
        }
        boolean asc = "asc".equalsIgnoreCase(order);
        wrapper.orderBy(true, asc, column);
        return true;
    }
}
