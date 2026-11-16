package cn.sichu.system.auth.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
public class PasswordForm {

    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 100, message = "新密码长度须为 8~100 位")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
