package cn.sichu.quartz.service;

import cn.sichu.common.result.PageResult;
import cn.sichu.quartz.entity.SysJobLog;
import cn.sichu.quartz.model.query.SysJobQuery;
import cn.sichu.quartz.model.vo.SysJobLogVO;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public interface IJobLogService {

    PageResult<SysJobLogVO> page(SysJobQuery query);

    void record(SysJobLog entry);
}
