package cn.sichu.system.role.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class SysRoleForm {

    private String appCode = "workspace";

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50)
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 100)
    private String roleName;

    private Integer dataScope = 5;

    private Integer roleSort = 0;

    private Boolean menuCheckStrictly = true;

    private Integer status = 1;

    @Size(max = 500)
    private String remark;

    private List<Long> menuIds;

    private List<Long> deptIds;
}
