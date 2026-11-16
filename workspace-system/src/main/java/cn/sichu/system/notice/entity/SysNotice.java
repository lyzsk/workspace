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
@TableName("sys_notice")
public class SysNotice extends TenantEntity {

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

    private Long publisherId;

    private LocalDateTime publishTime;

    private LocalDateTime remindTime;

    private LocalDateTime expireTime;
}
