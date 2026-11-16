package cn.sichu.quartz.model.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
public record CronValidationVO(boolean valid, String message, String rule,
                               List<LocalDateTime> nextTimes) {
}
