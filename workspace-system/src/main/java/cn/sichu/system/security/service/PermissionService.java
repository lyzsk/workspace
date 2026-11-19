package cn.sichu.system.security.service;

import cn.sichu.system.app.grant.service.IAppAuthorizationService;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.common.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Component("ss")
@RequiredArgsConstructor
public class PermissionService {

    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final IAppAuthorizationService authorizationService;

    public boolean hasPerm(String permission) {
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        if (loginUser == null || !authorizationService.getAuthorization(
            loginUser.getUserId(), "workspace").isAccessible()) {
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
