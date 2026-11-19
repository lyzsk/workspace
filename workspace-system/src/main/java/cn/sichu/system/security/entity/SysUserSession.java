package cn.sichu.system.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@Data
@TableName("sys_user_session")
public class SysUserSession {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long userId;

    private String jti;

    private String sessionCode;

    private String refreshFamily;

    private Long tokenVersion;

    private LocalDateTime loginTime;

    private LocalDateTime lastSeenAt;

    private LocalDateTime expiresAt;

    private String ip;

    private String userAgent;

    private Integer revoked;

    private LocalDateTime revokedAt;
}
