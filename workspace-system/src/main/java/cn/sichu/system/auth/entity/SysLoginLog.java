package cn.sichu.system.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
@TableName("sys_login_log")
public class SysLoginLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long userId;

    private String username;

    private String loginType;

    private Integer status;

    private String message;

    private String ip;

    private String userAgent;

    private LocalDateTime loginTime;
}
