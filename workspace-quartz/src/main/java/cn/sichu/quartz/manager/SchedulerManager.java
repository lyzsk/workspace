package cn.sichu.quartz.manager;

import cn.sichu.common.exception.BusinessException;
import cn.sichu.common.result.ResultCode;
import cn.sichu.quartz.entity.SysJob;
import cn.sichu.quartz.handler.JobExecution;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Service
@RequiredArgsConstructor
public class SchedulerManager {

    private final Scheduler scheduler;

    public void addJob(SysJob job) {
        try {
            if (exists(job)) {
                return;
            }
            scheduler.scheduleJob(jobDetail(job), cronTrigger(job));
        } catch (SchedulerException e) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                "任务注册失败: " + e.getMessage());
        }
    }

    public void updateJob(SysJob job) {
        deleteJob(job);
        if (job.getStatus() != null && job.getStatus() == 1) {
            addJob(job);
        }
    }

    public void deleteJob(SysJob job) {
        try {
            JobKey key = jobKey(job);
            if (scheduler.checkExists(key)) {
                scheduler.deleteJob(key);
            }
        } catch (SchedulerException e) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                "任务删除失败: " + e.getMessage());
        }
    }

    public void pauseJob(SysJob job) {
        try {
            JobKey key = jobKey(job);
            if (scheduler.checkExists(key)) {
                scheduler.pauseJob(key);
            }
        } catch (SchedulerException e) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                "任务暂停失败: " + e.getMessage());
        }
    }

    public void resumeJob(SysJob job) {
        try {
            JobKey key = jobKey(job);
            if (scheduler.checkExists(key)) {
                scheduler.resumeJob(key);
            }
        } catch (SchedulerException e) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                "任务恢复失败: " + e.getMessage());
        }
    }

    public void triggerJob(SysJob job) {
        try {
            JobKey key = jobKey(job);
            if (!scheduler.checkExists(key)) {
                scheduler.addJob(jobDetail(job), true);
            }
            scheduler.triggerJob(key, jobData(job));
        } catch (SchedulerException e) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                "任务执行失败: " + e.getMessage());
        }
    }

    public void reconcileWith(List<SysJob> jobs) {
        try {
            Set<JobKey> expected = jobs.stream().map(this::jobKey).collect(Collectors.toSet());
            for (JobKey existing : scheduler.getJobKeys(GroupMatcher.anyGroup())) {
                if (!expected.contains(existing)) {
                    scheduler.deleteJob(existing);
                }
            }
            for (SysJob job : jobs) {
                updateJob(job);
            }
        } catch (SchedulerException e) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                "任务全量对账失败: " + e.getMessage());
        }
    }

    public boolean exists(SysJob job) {
        try {
            return scheduler.checkExists(jobKey(job));
        } catch (SchedulerException e) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                "任务检查失败: " + e.getMessage());
        }
    }

    private JobKey jobKey(SysJob job) {
        return JobKey.jobKey("JOB_" + job.getId(), job.getJobGroup());
    }

    private TriggerKey triggerKey(SysJob job) {
        return TriggerKey.triggerKey("TRG_JOB_" + job.getId(), job.getJobGroup());
    }

    private JobDetail jobDetail(SysJob job) {
        return JobBuilder.newJob(JobExecution.class).withIdentity(jobKey(job))
            .usingJobData(jobData(job)).storeDurably().build();
    }

    private CronTrigger cronTrigger(SysJob job) {
        CronScheduleBuilder builder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
        Integer policy = job.getMisfirePolicy();
        if (policy != null && policy == 0) {
            builder.withMisfireHandlingInstructionIgnoreMisfires();
        } else if (policy != null && policy == 1) {
            builder.withMisfireHandlingInstructionFireAndProceed();
        } else {
            builder.withMisfireHandlingInstructionDoNothing();
        }
        return TriggerBuilder.newTrigger().withIdentity(triggerKey(job)).withSchedule(builder)
            .build();
    }

    private JobDataMap jobData(SysJob job) {
        JobDataMap data = new JobDataMap();
        data.put("JOB_ID", job.getId());
        data.put("JOB_NAME", job.getJobName());
        data.put("JOB_HANDLER_NAME", job.getHandlerName());
        data.put("JOB_HANDLER_PARAM", job.getParams());
        return data;
    }
}
