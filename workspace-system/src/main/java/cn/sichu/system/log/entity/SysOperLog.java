package cn.sichu.system.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long userId;

    private String username;

    private String module;

    private String operation;

    private String requestMethod;

    private String requestUri;

    private String requestParams;

    private String responseResult;

    private Integer status;

    private String errorMsg;

    private Long costTime;

    private String ip;

    private LocalDateTime operTime;
}
