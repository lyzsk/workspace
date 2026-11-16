package cn.sichu.quartz.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class SysJobVO {

    private String id;

    private String jobName;

    private String jobGroup;

    private String cronExpression;

    private String handlerName;

    private String params;

    private Integer misfirePolicy;

    private Integer status;

    private LocalDateTime createTime;
}
