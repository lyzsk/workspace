package cn.sichu.system.apiclient.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
@Data
public class ApiClientVO {

    private String id;

    private String clientId;

    private String clientName;

    private String scopes;

    private Integer status;

    private LocalDateTime expiresAt;

    private LocalDateTime lastUsedAt;

    private LocalDateTime createTime;
}
