package cn.sichu.system.online.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class SysOnlineUserVO {

    private String id;

    private String userId;

    private String username;

    private String realName;

    private String ip;

    private String userAgent;

    private LocalDateTime loginTime;

    private LocalDateTime lastSeenAt;

    private LocalDateTime expiresAt;
}
