package cn.sichu.system.notice.model.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
public class SysNoticeForm {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @NotBlank
    @Pattern(regexp = "TODO|ANNOUNCEMENT|MESSAGE")
    private String noticeType = "TODO";

    @Min(0)
    @Max(2)
    private Integer priority = 0;

    @Min(0)
    private Long repeatIntervalSeconds = 0L;

    @Min(1)
    private Integer maxRemindCount = 1;

    @Size(max = 100)
    private String templateCode;

    private Map<String, String> templateParams;

    @NotBlank
    @Pattern(regexp = "ALL|ROLE|DEPT")
    private String targetType = "ALL";

    private List<Long> targetIds;

    private LocalDateTime remindTime;

    private LocalDateTime expireTime;
}
