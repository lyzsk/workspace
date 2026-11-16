package cn.sichu.system.notice.entity;

import cn.sichu.common.base.TenantEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
public class SysMessage extends TenantEntity {

    private Long noticeId;

    private Long receiverId;

    private String category;

    private Integer level;

    private String linkUrl;

    private Integer readFlag;

    private LocalDateTime readTime;

    private Integer doneFlag;

    private LocalDateTime doneTime;

    private LocalDateTime dueTime;

    private LocalDateTime nextRemindTime;

    private LocalDateTime lastRemindTime;

    private Integer remindCount;
}
