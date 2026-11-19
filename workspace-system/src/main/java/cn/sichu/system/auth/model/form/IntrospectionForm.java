package cn.sichu.system.auth.model.form;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public record IntrospectionForm(
    @NotBlank String token
) {
}
