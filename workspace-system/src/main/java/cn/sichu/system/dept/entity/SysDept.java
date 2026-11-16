package cn.sichu.system.dept.entity;

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
@TableName("sys_dept")
public class SysDept extends TenantEntity {

    private String deptCode;

    private String deptName;

    private Long parentId;

    private Integer sortNo;

    private Integer status;
}
