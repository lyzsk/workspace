package cn.sichu.quartz.service.impl;

import cn.sichu.quartz.entity.SysJob;
import cn.sichu.quartz.handler.JobHandler;
import cn.sichu.quartz.manager.SchedulerManager;
import cn.sichu.quartz.mapper.SysJobMapper;
import cn.sichu.quartz.model.form.SysJobForm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.quartz.CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
class JobServiceImplTest {

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createRegistersSchedulerOnlyAfterCommit() throws Exception {
        Scheduler scheduler = mock(Scheduler.class);
        SchedulerManager schedulerManager = new SchedulerManager(scheduler);
        SysJobMapper jobMapper = mock(SysJobMapper.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, SysJob.class).setId(8L);
            return 1;
        }).when(jobMapper).insert(any(SysJob.class));
        JobServiceImpl service = new JobServiceImpl(schedulerManager,
            Map.of("demoJob", mock(JobHandler.class)));
        ReflectionTestUtils.setField(service, "baseMapper", jobMapper);
        TransactionSynchronizationManager.initSynchronization();

        service.create(form());

        verify(scheduler, never()).scheduleJob(any(), any());
        List<TransactionSynchronization> synchronizations =
            TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        verify(scheduler).scheduleJob(any(), any());
    }

    private SysJobForm form() {
        SysJobForm form = new SysJobForm();
        form.setJobName("demo");
        form.setJobGroup("SYSTEM");
        form.setCronExpression("0 0 0 * * ?");
        form.setHandlerName("demoJob");
        form.setMisfirePolicy(MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
        form.setStatus(1);
        return form;
    }
}
