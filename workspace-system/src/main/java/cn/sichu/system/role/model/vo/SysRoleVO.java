package cn.sichu.system.role.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class SysRoleVO {

    private String id;

    private String appCode;

    private String roleCode;

    private String roleName;

    private Integer dataScope;

    private Integer roleSort;

    private Boolean menuCheckStrictly;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private List<String> deptIds;
}
