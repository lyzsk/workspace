package cn.sichu.quartz.model.query;

import cn.sichu.common.base.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysJobQuery extends BaseQuery {

    private String jobName;

    private Integer status;
}
