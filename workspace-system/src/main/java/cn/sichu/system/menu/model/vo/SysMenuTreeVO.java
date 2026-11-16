package cn.sichu.system.menu.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class SysMenuTreeVO {

    private String id;

    private String parentId;

    private String menuCode;

    private String menuName;

    private String menuType;

    private String path;

    private String component;

    private String permission;

    private String icon;

    private Integer sortNo;

    private Integer hidden;

    private Integer status;

    private List<SysMenuTreeVO> children = new ArrayList<>();
}
