package cn.sichu.system.apiclient.entity;

import cn.sichu.common.core.base.TenantEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_api_client")
public class SysApiClient extends TenantEntity {

    private String clientId;

    private String clientName;

    private String secretHash;

    private String scopes;

    private Integer status;

    private LocalDateTime expiresAt;

    private LocalDateTime lastUsedAt;
}
