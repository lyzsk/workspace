package cn.sichu.system.role.model.query;

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
public class SysRoleQuery extends BaseQuery {

    private String roleCode;

    private String roleName;

    private Integer status;
}
