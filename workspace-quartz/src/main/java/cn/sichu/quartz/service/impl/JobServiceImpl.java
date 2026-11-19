package cn.sichu.quartz.service.impl;

import cn.sichu.common.core.base.BaseImpl;
import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.core.result.PageResult;
import cn.sichu.common.core.result.ResultCode;
import cn.sichu.quartz.entity.SysJob;
import cn.sichu.quartz.handler.JobHandler;
import cn.sichu.quartz.manager.SchedulerManager;
import cn.sichu.quartz.mapper.SysJobMapper;
import cn.sichu.quartz.model.form.SysJobForm;
import cn.sichu.quartz.model.query.SysJobQuery;
import cn.sichu.quartz.model.vo.SysJobVO;
import cn.sichu.quartz.service.IJobService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.quartz.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Service
@RequiredArgsConstructor
public class JobServiceImpl extends BaseImpl<SysJobMapper, SysJob>
    implements IJobService {

    private final SchedulerManager schedulerManager;
    private final Map<String, JobHandler> handlers;

    @Override
    public PageResult<SysJobVO> page(SysJobQuery query) {
        Page<SysJob> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysJob> wrapper =
            new LambdaQueryWrapper<SysJob>().like(
                    StringUtils.hasText(query.getJobName()), SysJob::getJobName,
                    query.getJobName())
                .eq(query.getStatus() != null, SysJob::getStatus,
                    query.getStatus()).orderByDesc(SysJob::getCreateTime);
        Page<SysJob> result = baseMapper.selectPage(page, wrapper);
        List<SysJobVO> records =
            result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(),
            result.getSize());
    }

    @Override
    @Transactional
    public Long create(SysJobForm form) {
        validate(form);
        SysJob job = new SysJob();
        apply(job, form);
        baseMapper.insert(job);
        if (job.getStatus() != null && job.getStatus() == 1) {
            afterCommit(() -> schedulerManager.addJob(job));
        }
        return job.getId();
    }

    @Override
    @Transactional
    public void update(Long id, SysJobForm form) {
        SysJob job = required(id);
        validate(form);
        apply(job, form);
        baseMapper.updateById(job);
        afterCommit(() -> schedulerManager.updateJob(job));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SysJob job = required(id);
        baseMapper.deleteById(id);
        afterCommit(() -> schedulerManager.deleteJob(job));
    }

    @Override
    @Transactional
    public void pause(Long id) {
        SysJob job = required(id);
        job.setStatus(0);
        baseMapper.updateById(job);
        afterCommit(() -> schedulerManager.pauseJob(job));
    }

    @Override
    @Transactional
    public void resume(Long id) {
        SysJob job = required(id);
        job.setStatus(1);
        baseMapper.updateById(job);
        if (!schedulerManager.exists(job)) {
            afterCommit(() -> schedulerManager.addJob(job));
        } else {
            afterCommit(() -> schedulerManager.resumeJob(job));
        }
    }

    @Override
    public void runNow(Long id) {
        schedulerManager.triggerJob(required(id));
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
    }

    private SysJob required(Long id) {
        SysJob job = baseMapper.selectById(id);
        if (job == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在");
        }
        return job;
    }

    private void validate(SysJobForm form) {
        if (!CronExpression.isValidExpression(form.getCronExpression())) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                "cron 表达式无效");
        }
        if (!handlers.containsKey(form.getHandlerName())) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                "任务处理器不存在: " + form.getHandlerName());
        }
    }

    private void apply(SysJob job, SysJobForm form) {
        job.setJobName(form.getJobName());
        job.setJobGroup(
            form.getJobGroup() == null || form.getJobGroup().isBlank() ?
                "SYSTEM" : form.getJobGroup());
        job.setCronExpression(form.getCronExpression());
        job.setHandlerName(form.getHandlerName());
        job.setParams(form.getParams());
        job.setMisfirePolicy(
            form.getMisfirePolicy() == null ? 1 : form.getMisfirePolicy());
        job.setStatus(form.getStatus() == null ? 0 : form.getStatus());
    }

    private SysJobVO toVO(SysJob job) {
        SysJobVO vo = new SysJobVO();
        vo.setId(job.getId() == null ? null : job.getId().toString());
        vo.setJobName(job.getJobName());
        vo.setJobGroup(job.getJobGroup());
        vo.setCronExpression(job.getCronExpression());
        vo.setHandlerName(job.getHandlerName());
        vo.setParams(job.getParams());
        vo.setMisfirePolicy(job.getMisfirePolicy());
        vo.setStatus(job.getStatus());
        vo.setCreateTime(job.getCreateTime());
        return vo;
    }
}
