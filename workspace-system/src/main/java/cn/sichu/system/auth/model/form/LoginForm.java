package cn.sichu.system.auth.model.form;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public record LoginForm(
    @NotBlank(message = "用户名不能为空") String username,
    @NotBlank(message = "密码不能为空") String password
) {
}
