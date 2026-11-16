package cn.sichu.tool.document.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
@TableName("sys_document_operation")
public class SysDocumentOperation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long documentId;

    private Long revision;

    private String opType;

    private String targetType;

    private String targetId;

    private String payload;

    private Long operatorId;

    private LocalDateTime createTime;
}
