package cn.sichu.quartz.entity;

import cn.sichu.common.core.base.TenantEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job")
public class SysJob extends TenantEntity {

    private String jobName;

    private String jobGroup;

    private String cronExpression;

    private String handlerName;

    private String params;

    private Integer misfirePolicy;

    private Integer status;
}
