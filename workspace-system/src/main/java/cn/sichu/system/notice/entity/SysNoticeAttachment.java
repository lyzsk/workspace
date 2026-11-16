package cn.sichu.system.notice.entity;

import cn.sichu.common.base.TenantEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice_attachment")
public class SysNoticeAttachment extends TenantEntity {

    private Long noticeId;

    private Long fileId;

    private Integer sortNo;
}
