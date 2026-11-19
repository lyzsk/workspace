package cn.sichu.system.app.grant.entity;

import cn.sichu.common.base.TenantEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_app")
public class SysUserApp extends TenantEntity {

    private Long userId;

    private Long appId;

    private Integer status;

    private Integer dataScope;
}
