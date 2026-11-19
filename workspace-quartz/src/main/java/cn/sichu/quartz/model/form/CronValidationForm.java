package cn.sichu.quartz.model.form;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
public record CronValidationForm(
    @NotBlank(message = "cron 表达式不能为空") String expression) {
}
