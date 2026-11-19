package cn.sichu.quartz.handler;

import cn.sichu.common.utils.TextUtils;
import cn.sichu.quartz.entity.SysJobLog;
import cn.sichu.quartz.service.IJobLogService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Slf4j
@Component
@DisallowConcurrentExecution
public class JobExecution extends QuartzJobBean {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    protected void executeInternal(JobExecutionContext context)
        throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();
        Long jobId = data.getLong("JOB_ID");
        String jobName = data.getString("JOB_NAME");
        String handlerName = data.getString("JOB_HANDLER_NAME");
        String params = data.getString("JOB_HANDLER_PARAM");
        long start = System.currentTimeMillis();

        SysJobLog entry = new SysJobLog();
        entry.setTenantId(0L);
        entry.setJobId(jobId);
        entry.setJobName(jobName);
        entry.setHandlerName(handlerName);
        entry.setParams(params);
        entry.setStartTime(LocalDateTime.now());
        entry.setStatus(1);

        try {
            applicationContext.getBean(handlerName, JobHandler.class)
                .execute(params);
            entry.setCostTime(System.currentTimeMillis() - start);
        } catch (Exception e) {
            entry.setStatus(0);
            entry.setErrorMsg(TextUtils.truncate(e.getMessage(), 2000));
            entry.setCostTime(System.currentTimeMillis() - start);
            save(entry);
            throw new JobExecutionException(e);
        }
        save(entry);
    }

    private void save(SysJobLog entry) {
        entry.setEndTime(LocalDateTime.now());
        applicationContext.getBean(IJobLogService.class).record(entry);
    }
}
