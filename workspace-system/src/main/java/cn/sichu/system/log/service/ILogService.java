package cn.sichu.system.log.service;

import cn.sichu.common.result.PageResult;
import cn.sichu.system.log.entity.SysOperLog;
import cn.sichu.system.log.model.query.SysLoginLogQuery;
import cn.sichu.system.log.model.query.SysOperLogQuery;
import cn.sichu.system.log.model.vo.SysLoginLogVO;
import cn.sichu.system.log.model.vo.SysOperLogVO;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public interface ILogService {

    PageResult<SysLoginLogVO> pageLoginLogs(SysLoginLogQuery query);

    PageResult<SysOperLogVO> pageOperLogs(SysOperLogQuery query);

    void record(SysOperLog operLog);

    List<SysLoginLogVO> listLoginLogs(SysLoginLogQuery query);

    List<SysOperLogVO> listOperLogs(SysOperLogQuery query);
}
