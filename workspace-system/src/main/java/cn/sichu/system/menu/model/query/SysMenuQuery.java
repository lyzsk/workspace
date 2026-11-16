package cn.sichu.system.menu.model.query;

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
public class SysMenuQuery extends BaseQuery {

    private String menuName;

    private String menuType;

    private Integer status;

    private String appCode;
}
