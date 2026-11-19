package cn.sichu.auth.model.vo;

import cn.sichu.common.security.model.LoginUser;

import java.util.Set;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
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

    public static UserInfoVO from(LoginUser user, String tenantName, String deptName) {
        return new UserInfoVO(user.getUserId(), user.getUsername(), user.getRealName(),
            user.getGender(), user.getEmail(), user.getMobile(), user.getAvatar(),
            user.getTenantId(), tenantName, user.getDeptId(), deptName,
            user.getRoles(), user.getPermissions());
    }
}
