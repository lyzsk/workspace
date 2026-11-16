package cn.sichu.system.auth.model.vo;

import cn.sichu.system.security.model.LoginUser;

import java.util.Set;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public record UserInfoVO(
    Long id,
    String username,
    String realName,
    Integer gender,
    String email,
    String mobile,
    String avatar,
    Long tenantId,
    String tenantName,
    Long deptId,
    String deptName,
    Set<String> roles,
    Set<String> permissions
) {

    public static UserInfoVO from(LoginUser user, UserOrgVO org) {
        return new UserInfoVO(user.getUserId(), user.getUsername(), user.getRealName(),
            user.getGender(), user.getEmail(), user.getMobile(), user.getAvatar(),
            user.getTenantId(), org.tenantName(), user.getDeptId(), org.deptName(),
            user.getRoles(), user.getPermissions());
    }
}
