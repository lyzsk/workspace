package cn.sichu.common.security.model;

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
    @com.fasterxml.jackson.annotation.JsonIgnore
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

    public LoginUser(LoginUserSource user, Collection<String> roles,
        Collection<String> permissions) {
        this(user.getId(), user.getUsername(), user.getPassword(),
            user.getRealName(), user.getEmail(), user.getMobile(),
            user.getAvatar(), user.getTenantId(), user.getDeptId(),
            user.getGender(), user.getStatus(), user.getTokenVersion(),
            user.getLockedUntil(), roles, permissions);
    }

    @com.fasterxml.jackson.annotation.JsonCreator
    public LoginUser(
        @com.fasterxml.jackson.annotation.JsonProperty("userId") Long userId,
        @com.fasterxml.jackson.annotation.JsonProperty("username")
        String username,
        @com.fasterxml.jackson.annotation.JsonProperty("password")
        String password,
        @com.fasterxml.jackson.annotation.JsonProperty("realName")
        String realName,
        @com.fasterxml.jackson.annotation.JsonProperty("email") String email,
        @com.fasterxml.jackson.annotation.JsonProperty("mobile") String mobile,
        @com.fasterxml.jackson.annotation.JsonProperty("avatar") String avatar,
        @com.fasterxml.jackson.annotation.JsonProperty("tenantId")
        Long tenantId,
        @com.fasterxml.jackson.annotation.JsonProperty("deptId") Long deptId,
        @com.fasterxml.jackson.annotation.JsonProperty("gender") Integer gender,
        @com.fasterxml.jackson.annotation.JsonProperty("status") Integer status,
        @com.fasterxml.jackson.annotation.JsonProperty("tokenVersion")
        Long tokenVersion,
        @com.fasterxml.jackson.annotation.JsonProperty("lockedUntil")
        LocalDateTime lockedUntil,
        @com.fasterxml.jackson.annotation.JsonProperty("roles")
        Collection<String> roles,
        @com.fasterxml.jackson.annotation.JsonProperty("permissions")
        Collection<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.email = email;
        this.mobile = mobile;
        this.avatar = avatar;
        this.tenantId = tenantId;
        this.deptId = deptId;
        this.gender = gender;
        this.status = status;
        this.tokenVersion = tokenVersion == null ? 0L : tokenVersion;
        this.lockedUntil = lockedUntil;
        this.roles = new LinkedHashSet<>(roles == null ? Set.of() : roles);
        this.permissions =
            new LinkedHashSet<>(permissions == null ? Set.of() : permissions);
    }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getPassword() {
        return password;
    }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Stream.concat(roles.stream().map(role -> "ROLE_" + role),
            permissions.stream()).map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isAccountNonLocked() {
        return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now());
    }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
