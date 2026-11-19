package cn.sichu.system.user.model.query;

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
public class SysUserQuery extends BaseQuery {

    private String username;

    private Integer gender;

    private Integer status;
}
