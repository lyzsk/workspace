package cn.sichu.system.log.model.query;

import cn.sichu.common.base.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysOperLogQuery extends BaseQuery {

    private String username;

    private String module;

    private Integer status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime beginTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;
}
