package cn.sichu.system.security.entity;

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
@TableName("sys_token_blacklist")
public class SysTokenBlacklist {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String jti;

    private LocalDateTime expiresAt;

    private LocalDateTime createTime;
}
