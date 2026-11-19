package cn.sichu.auth.model.dto;

import cn.sichu.common.security.model.LoginUser;
import cn.sichu.common.security.model.LoginUserSource;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Data
public class LoginCredentialsDTO implements LoginUserSource {

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
    private Set<String> roles;
    private Set<String> permissions;

    public LoginUser toLoginUser() {
        return new LoginUser(this, roles, permissions);
    }
}
