package cn.sichu.system.log.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class SysOperLogVO {

    private String id;

    private String username;

    private String deptName;

    private String module;

    private String operation;

    private String method;

    private String requestMethod;

    private String requestUri;

    private String requestParams;

    private String requestQuery;

    private String responseResult;

    private Integer status;

    private String errorMsg;

    private String errorType;

    private Long costTime;

    private String ip;

    private String userAgent;

    private LocalDateTime operTime;
}
