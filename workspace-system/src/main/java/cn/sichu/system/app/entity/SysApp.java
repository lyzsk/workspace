package cn.sichu.system.app.entity;

import cn.sichu.common.base.BaseEntity;
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
@TableName("sys_app")
public class SysApp extends BaseEntity {

    private String code;

    private String name;

    private String icon;

    private String basePath;

    private String moduleEntry;

    private String styleEntry;

    private String accessPermission;

    private Integer sortNo;

    private Integer status;
}
