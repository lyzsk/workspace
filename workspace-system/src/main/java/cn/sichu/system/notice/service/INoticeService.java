package cn.sichu.system.notice.service;

import cn.sichu.common.core.result.PageResult;
import cn.sichu.system.notice.model.form.SysNoticeForm;
import cn.sichu.system.notice.model.form.SysNoticePublishForm;
import cn.sichu.system.notice.model.query.SysNoticeQuery;
import cn.sichu.system.notice.model.vo.SysNoticeVO;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
public interface INoticeService {

    PageResult<SysNoticeVO> page(SysNoticeQuery query);

    Long create(SysNoticeForm form);

    int publish(Long id, SysNoticePublishForm form);
}
