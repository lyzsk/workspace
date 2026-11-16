package cn.sichu.system.menu.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class SysMenuForm {

    @Size(max = 50)
    private String appCode = "platform";

    @NotBlank(message = "菜单编码不能为空")
    @Size(max = 100)
    private String menuCode;

    private Long parentId = 0L;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 100)
    private String menuName;

    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    @Size(max = 200)
    private String path;

    @Size(max = 200)
    private String component;

    @Size(max = 100)
    private String permission;

    @Size(max = 100)
    private String icon;

    private Integer sortNo = 0;

    private Integer hidden = 1;

    private Integer status = 1;
}
