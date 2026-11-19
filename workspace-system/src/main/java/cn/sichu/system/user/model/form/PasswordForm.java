package cn.sichu.system.user.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Data
public class PasswordForm {

    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100)
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
