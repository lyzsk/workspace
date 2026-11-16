package cn.sichu.system.user.entity;

import cn.sichu.common.base.TenantEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_role")
public class SysUserRole extends TenantEntity {

    private Long userId;

    private Long roleId;
}
