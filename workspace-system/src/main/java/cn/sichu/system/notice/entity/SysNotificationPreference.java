package cn.sichu.system.notice.entity;

import cn.sichu.common.base.TenantEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notification_preference")
public class SysNotificationPreference extends TenantEntity {

    private Long userId;

    private Integer quietEnabled;

    private LocalTime quietStart;

    private LocalTime quietEnd;

    private String timezone;
}
