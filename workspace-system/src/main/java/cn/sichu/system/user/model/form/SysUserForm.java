package cn.sichu.system.user.model.form;

import jakarta.validation.constraints.Email;
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
public class SysUserForm {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名不能超过 50 个字符")
    private String username;

    @Size(max = 100, message = "密码不能超过 100 个字符")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名不能超过 50 个字符")
    private String realName;

    /* 0=未知，1=男，2=女 */
    private Integer gender;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过 100 个字符")
    private String email;

    @Size(max = 20, message = "手机号不能超过 20 个字符")
    private String mobile;

    @Size(max = 500, message = "头像地址不能超过 500 个字符")
    private String avatar;

    private Long deptId;

    private Integer status;

    private List<Long> roleIds;
}
