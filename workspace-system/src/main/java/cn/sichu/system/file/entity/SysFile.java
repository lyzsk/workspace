package cn.sichu.system.file.entity;

import cn.sichu.common.core.base.TenantEntity;
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
@TableName("sys_file")
public class SysFile extends TenantEntity {

    private String originalName;

    private String storedName;

    private String storageKey;

    private String contentType;

    private Long sizeBytes;

    private String sha256;

    private Long uploaderId;
}
