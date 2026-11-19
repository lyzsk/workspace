package cn.sichu.system.auth.model.vo;

import cn.sichu.common.security.model.LoginUser;
import cn.sichu.common.security.model.LoginUserSource;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Data
public class LoginCredentialsVO implements LoginUserSource {

    private Long id;
    private String username;
    private String password;
    private String realName;
    private String email;
    private String mobile;
    private String avatar;
    private Long tenantId;
    private Long deptId;
    private Integer gender;
    private Integer status;
    private Long tokenVersion;
    private LocalDateTime lockedUntil;
    private java.util.Set<String> roles;
    private java.util.Set<String> permissions;

    public static LoginCredentialsVO from(LoginUser user) {
        LoginCredentialsVO result = new LoginCredentialsVO();
        result.setId(user.getUserId());
        result.setUsername(user.getUsername());
        result.setPassword(user.getPassword());
        result.setRealName(user.getRealName());
        result.setEmail(user.getEmail());
        result.setMobile(user.getMobile());
        result.setAvatar(user.getAvatar());
        result.setTenantId(user.getTenantId());
        result.setDeptId(user.getDeptId());
        result.setGender(user.getGender());
        result.setStatus(user.getStatus());
        result.setTokenVersion(user.getTokenVersion());
        result.setLockedUntil(user.getLockedUntil());
        result.setRoles(user.getRoles());
        result.setPermissions(user.getPermissions());
        return result;
    }

    public LoginUser toLoginUser() {
        return new LoginUser(this, roles, permissions);
    }
}
