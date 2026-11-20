package cn.sichu.system.apiclient.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
@Data
public class ApiClientForm {

    @NotBlank(message = "调用方名称不能为空")
    @Size(max = 100)
    private String clientName;

    @Size(max = 1000)
    private String scopes;

    private Integer status;

    private LocalDateTime expiresAt;

    @Size(max = 500)
    private String remark;
}
