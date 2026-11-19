package cn.sichu.system.dict.model.query;

import cn.sichu.common.core.base.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictDataQuery extends BaseQuery {

    private String dictType;

    private String label;

    private Integer status;
}
