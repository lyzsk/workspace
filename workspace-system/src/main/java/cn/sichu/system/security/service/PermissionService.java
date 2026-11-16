package cn.sichu.system.security.service;

import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.util.SecurityUtils;
import org.springframework.stereotype.Component;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Component("ss")
public class PermissionService {

    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    public boolean hasPerm(String permission) {
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        if (loginUser == null) {
            return false;
        }
        if (loginUser.getRoles().contains(SUPER_ADMIN_ROLE)) {
            return true;
        }
        return loginUser.getPermissions().contains(permission);
    }

    public boolean hasRole(String roleCode) {
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        return loginUser != null && loginUser.getRoles().contains(roleCode);
    }
}
