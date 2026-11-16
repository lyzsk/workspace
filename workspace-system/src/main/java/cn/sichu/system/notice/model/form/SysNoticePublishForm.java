package cn.sichu.system.notice.model.form;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
public class SysNoticePublishForm {

    @Size(max = 500)
    private String linkUrl;

    private LocalDateTime dueTime;
}
