package cn.sichu.system.role.entity;

import cn.sichu.common.base.TenantEntity;
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
@TableName("sys_role")
public class SysRole extends TenantEntity {

    private Long appId;

    private String roleCode;

    private String roleName;

    private Integer dataScope;

    private Integer roleSort;

    private Boolean menuCheckStrictly;

    private Integer status;
}
