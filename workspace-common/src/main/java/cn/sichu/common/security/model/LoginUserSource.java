package cn.sichu.common.security.model;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public interface LoginUserSource {

    Long getId();

    String getUsername();

    String getPassword();

    String getRealName();

    String getEmail();

    String getMobile();

    String getAvatar();

    Long getTenantId();

    Long getDeptId();

    Integer getGender();

    Integer getStatus();

    Long getTokenVersion();

    LocalDateTime getLockedUntil();
}
