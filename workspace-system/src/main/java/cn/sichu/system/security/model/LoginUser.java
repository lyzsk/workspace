package cn.sichu.system.security.model;

import cn.sichu.system.user.entity.SysUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Getter
public class LoginUser implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private final String password;
    private final String realName;
    private final String email;
    private final String mobile;
    private final String avatar;
    private final Long tenantId;
    private final Long deptId;
    private final Integer gender;
    private final Integer status;
    private final Long tokenVersion;
    private final LocalDateTime lockedUntil;
    private final Set<String> roles;
    private final Set<String> permissions;

    public LoginUser(SysUser user, Collection<String> roles, Collection<String> permissions) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.realName = user.getRealName();
        this.email = user.getEmail();
        this.mobile = user.getMobile();
        this.avatar = user.getAvatar();
        this.tenantId = user.getTenantId();
        this.deptId = user.getDeptId();
        this.gender = user.getGender();
        this.status = user.getStatus();
        this.tokenVersion = user.getTokenVersion() == null ? 0L : user.getTokenVersion();
        this.lockedUntil = user.getLockedUntil();
        this.roles = new LinkedHashSet<>(roles);
        this.permissions = new LinkedHashSet<>(permissions);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Stream.concat(
                roles.stream().map(role -> "ROLE_" + role),
                permissions.stream())
            .map(SimpleGrantedAuthority::new)
            .toList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
