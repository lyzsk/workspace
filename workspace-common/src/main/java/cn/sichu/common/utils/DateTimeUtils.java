package cn.sichu.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
public final class DateTimeUtils {

    private static final DateTimeFormatter DATE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private static final DateTimeFormatter FILE_STAMP =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private DateTimeUtils() {
    }

    public static String format(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    public static String fileStamp(LocalDateTime value) {
        return FILE_STAMP.format(value == null ? LocalDateTime.now() : value);
    }
}
