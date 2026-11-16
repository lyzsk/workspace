package cn.sichu.system.notice.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
public class SysMessageVO {

    private Long id;

    private Long noticeId;

    private String title;

    private String content;

    private String category;

    private Integer level;

    private String linkUrl;

    private Integer readFlag;

    private LocalDateTime readTime;

    private Integer doneFlag;

    private LocalDateTime doneTime;

    private LocalDateTime dueTime;

    private LocalDateTime createTime;
}
