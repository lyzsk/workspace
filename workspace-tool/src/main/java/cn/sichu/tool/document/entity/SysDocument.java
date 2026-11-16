package cn.sichu.tool.document.entity;

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
@TableName("sys_document")
public class SysDocument extends TenantEntity {

    private String title;

    private String docType;

    private Long ownerId;

    private String content;

    private Long revision;
}
