package cn.sichu.tool.document.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
public class SysDocumentVO {

    private Long id;

    private String title;

    private String docType;

    private Long ownerId;

    private String content;

    private Long revision;

    private String permission;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
