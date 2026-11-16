package cn.sichu.quartz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
@TableName("sys_job_log")
public class SysJobLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long jobId;

    private String jobName;

    private String handlerName;

    private String params;

    private Integer status;

    private String errorMsg;

    private Long costTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
