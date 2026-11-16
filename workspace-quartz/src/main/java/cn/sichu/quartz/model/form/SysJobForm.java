package cn.sichu.quartz.model.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class SysJobForm {

    @NotBlank(message = "任务名称不能为空")
    private String jobName;

    private String jobGroup = "SYSTEM";

    @NotBlank(message = "cron 表达式不能为空")
    private String cronExpression;

    @NotBlank(message = "处理器不能为空")
    private String handlerName;

    private String params;

    private Integer misfirePolicy = 1;

    private Integer status = 0;
}
