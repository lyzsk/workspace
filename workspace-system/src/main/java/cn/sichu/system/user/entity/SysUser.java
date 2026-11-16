package cn.sichu.system.user.entity;

import cn.sichu.common.base.TenantEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends TenantEntity {

    private String username;

    private String password;

    private String realName;

    private Integer gender;

    private String email;

    private String mobile;

    private String avatar;

    private Long deptId;

    private Integer status;

    private Long tokenVersion;

    private Integer loginFailCount;

    private LocalDateTime lockedUntil;
}
