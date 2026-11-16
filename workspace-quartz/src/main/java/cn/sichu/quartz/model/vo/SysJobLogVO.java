package cn.sichu.quartz.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class SysJobLogVO {

    private String id;

    private String jobId;

    private String jobName;

    private String handlerName;

    private String params;

    private Integer status;

    private String errorMsg;

    private Long costTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
