package cn.sichu.common.core.utils;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
public final class TextUtils {

    private TextUtils() {
    }

    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        int limit = Math.max(0, maxLength);
        if (value.codePointCount(0, value.length()) <= limit) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, limit));
    }
}
