package cn.sichu.common.base;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TenantEntity extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("tenant_id")
    private Long tenantId;
}
