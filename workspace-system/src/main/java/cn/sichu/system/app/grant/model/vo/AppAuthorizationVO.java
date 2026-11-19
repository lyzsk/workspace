package cn.sichu.system.app.grant.model.vo;

import cn.sichu.system.menu.model.vo.SysMenuTreeVO;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Data
public class AppAuthorizationVO {

    private String appCode;

    private boolean accessible;

    private Set<String> roles;

    private Set<String> permissions;

    private List<SysMenuTreeVO> menus;

    private Integer dataScope;

    private boolean all;

    private List<String> deptIds;

    private boolean selfOnly;

    private String userId;
}
