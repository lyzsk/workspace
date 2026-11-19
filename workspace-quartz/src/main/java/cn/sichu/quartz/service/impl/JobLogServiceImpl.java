package cn.sichu.quartz.service.impl;

import cn.sichu.common.base.BaseImpl;
import cn.sichu.common.result.PageResult;
import cn.sichu.quartz.entity.SysJobLog;
import cn.sichu.quartz.mapper.SysJobLogMapper;
import cn.sichu.quartz.model.query.SysJobQuery;
import cn.sichu.quartz.model.vo.SysJobLogVO;
import cn.sichu.quartz.service.IJobLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobLogServiceImpl extends BaseImpl<SysJobLogMapper, SysJobLog>
    implements IJobLogService {

    @Override
    public PageResult<SysJobLogVO> page(SysJobQuery query) {
        Page<SysJobLog> page =
            new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysJobLog> wrapper =
            new LambdaQueryWrapper<SysJobLog>().like(
                    StringUtils.hasText(query.getJobName()), SysJobLog::getJobName,
                    query.getJobName())
                .eq(query.getStatus() != null, SysJobLog::getStatus,
                    query.getStatus()).orderByDesc(SysJobLog::getStartTime);
        Page<SysJobLog> result = baseMapper.selectPage(page, wrapper);
        List<SysJobLogVO> records =
            result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(),
            result.getSize());
    }

    @Override
    public void record(SysJobLog entry) {
        try {
            baseMapper.insert(entry);
        } catch (Exception e) {
            log.warn("写任务日志失败: {}", e.getMessage());
        }
    }

    private SysJobLogVO toVO(SysJobLog entry) {
        SysJobLogVO vo = new SysJobLogVO();
        vo.setId(entry.getId() == null ? null : entry.getId().toString());
        vo.setJobId(
            entry.getJobId() == null ? null : entry.getJobId().toString());
        vo.setJobName(entry.getJobName());
        vo.setHandlerName(entry.getHandlerName());
        vo.setParams(entry.getParams());
        vo.setStatus(entry.getStatus());
        vo.setErrorMsg(entry.getErrorMsg());
        vo.setCostTime(entry.getCostTime());
        vo.setStartTime(entry.getStartTime());
        vo.setEndTime(entry.getEndTime());
        return vo;
    }
}
