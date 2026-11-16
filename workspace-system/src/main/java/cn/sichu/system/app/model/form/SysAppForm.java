package cn.sichu.system.app.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@Data
public class SysAppForm {

    @NotBlank(message = "应用编码不能为空")
    @Size(max = 50)
    private String code;

    @NotBlank(message = "应用名称不能为空")
    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String icon;

    @NotBlank(message = "路由前缀不能为空")
    @Size(max = 100)
    private String basePath;

    @NotBlank(message = "微前端模块入口不能为空")
    @Size(max = 500)
    private String moduleEntry;

    @Size(max = 500)
    private String styleEntry;

    @NotBlank(message = "应用访问权限码不能为空")
    @Size(max = 100)
    private String accessPermission;

    private Integer sortNo = 0;

    private Integer status = 1;

    @Size(max = 500)
    private String remark;
}
