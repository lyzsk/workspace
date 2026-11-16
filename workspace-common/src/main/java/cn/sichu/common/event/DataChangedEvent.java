package cn.sichu.common.event;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
public record DataChangedEvent(String path, String method, LocalDateTime time) {
}
