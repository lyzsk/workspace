package cn.sichu.system.menu.entity;

import cn.sichu.common.core.base.BaseEntity;
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
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    private String appCode;

    private String menuCode;

    private Long parentId;

    private String menuName;

    private String menuType;

    private String path;

    private String component;

    private String permission;

    private String icon;

    private Integer sortNo;

    private Integer hidden;

    private Integer status;
}
