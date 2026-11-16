package cn.sichu.system.notice.service;

import cn.sichu.common.result.PageResult;
import cn.sichu.system.notice.model.query.SysMessageQuery;
import cn.sichu.system.notice.model.vo.SysMessageVO;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
public interface IMessageService {

    PageResult<SysMessageVO> pageMy(SysMessageQuery query);

    int unreadCount();

    void read(Long id);

    int readAll();

    void done(Long id);
}
