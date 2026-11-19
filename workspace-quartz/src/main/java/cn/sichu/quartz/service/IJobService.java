package cn.sichu.quartz.service;

import cn.sichu.common.core.result.PageResult;
import cn.sichu.quartz.model.form.SysJobForm;
import cn.sichu.quartz.model.query.SysJobQuery;
import cn.sichu.quartz.model.vo.SysJobVO;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public interface IJobService {

    PageResult<SysJobVO> page(SysJobQuery query);

    Long create(SysJobForm form);

    void update(Long id, SysJobForm form);

    void delete(Long id);

    void pause(Long id);

    void resume(Long id);

    void runNow(Long id);
}
