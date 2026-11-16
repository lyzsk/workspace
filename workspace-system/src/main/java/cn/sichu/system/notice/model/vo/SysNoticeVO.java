package cn.sichu.system.notice.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
public class SysNoticeVO {

    private Long id;

    private String title;

    private String content;

    private String noticeType;

    private Integer priority;

    private Long repeatIntervalSeconds;

    private Integer maxRemindCount;

    private String templateCode;

    private String templateParams;

    private String targetType;

    private String targetIds;

    private Integer status;

    private LocalDateTime publishTime;

    private LocalDateTime remindTime;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;
}
