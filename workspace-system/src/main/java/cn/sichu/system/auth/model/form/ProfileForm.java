package cn.sichu.system.auth.model.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
public class ProfileForm {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50)
    private String realName;

    @Min(0)
    @Max(2)
    private Integer gender;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String mobile;

    @Pattern(regexp = "avatar-(default|male|female)", message = "头像不正确")
    private String avatar;
}
