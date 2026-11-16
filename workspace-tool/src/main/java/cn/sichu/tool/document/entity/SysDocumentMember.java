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
@TableName("sys_document_member")
public class SysDocumentMember extends TenantEntity {

    private Long documentId;

    private Long userId;

    private String permission;
}
