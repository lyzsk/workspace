package cn.sichu.system.role.entity;

import cn.sichu.common.core.base.TenantEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_dept")
public class SysRoleDept extends TenantEntity {

    private Long roleId;

    private Long deptId;
}
