package cn.sichu.tool.document.model.vo;

import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
public class SysDocumentMemberVO {

    private Long id;

    private Long userId;

    private String username;

    private String realName;

    private String permission;
}
