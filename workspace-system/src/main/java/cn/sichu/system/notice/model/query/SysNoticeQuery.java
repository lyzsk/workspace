package cn.sichu.system.notice.model.query;

import cn.sichu.common.core.base.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysNoticeQuery extends BaseQuery {

    private String title;

    private String noticeType;

    private Integer status;
}
