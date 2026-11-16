package cn.sichu.system.notice.model.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
public class SysNotificationPreferenceForm {

    @Min(0)
    @Max(1)
    private Integer quietEnabled = 0;

    private LocalTime quietStart;

    private LocalTime quietEnd;

    @NotBlank
    @Size(max = 64)
    private String timezone = "Asia/Shanghai";
}
