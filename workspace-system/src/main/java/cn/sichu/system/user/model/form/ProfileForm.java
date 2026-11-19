package cn.sichu.system.user.model.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Data
public class ProfileForm {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50)
    private String realName;

    private Integer gender;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String mobile;

    @Size(max = 500)
    private String avatar;
}
