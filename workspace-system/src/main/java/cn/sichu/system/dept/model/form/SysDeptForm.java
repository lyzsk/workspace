package cn.sichu.system.dept.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class SysDeptForm {

    @NotBlank(message = "部门编码不能为空")
    @Size(max = 50)
    private String deptCode;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 100)
    private String deptName;

    private Long parentId = 0L;

    private Integer sortNo = 0;

    private Integer status = 1;
}
