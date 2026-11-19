package cn.sichu.system.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Data
@TableName("sys_refresh_token")
public class SysRefreshToken {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long sessionId;

    private String tokenHash;

    private LocalDateTime issuedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime consumedAt;

    private LocalDateTime revokedAt;

    private String replacedByHash;
}
