package cn.sichu.system.auth.model.vo;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public record LoginVO(
    String token,
    UserInfoVO user
) {
}
