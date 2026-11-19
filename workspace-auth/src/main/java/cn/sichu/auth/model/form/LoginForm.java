package cn.sichu.auth.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public record LoginForm(@NotBlank(message = "用户名不能为空") @Size(max = 50) String username,
                        @NotBlank(message = "密码不能为空") @Size(max = 100) String password) {
}
