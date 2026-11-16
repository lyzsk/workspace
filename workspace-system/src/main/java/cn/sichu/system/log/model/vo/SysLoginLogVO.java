package cn.sichu.system.log.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class SysLoginLogVO {

    private String id;

    private String username;

    private Integer status;

    private String message;

    private String ip;

    private String userAgent;

    private LocalDateTime loginTime;
}
