package cn.sichu.quartz.manager;

import cn.sichu.quartz.entity.SysJob;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.quartz.CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
import static org.quartz.Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
class SchedulerManagerTest {

    @Test
    void addJobAppliesMisfirePolicy() throws Exception {
        Scheduler scheduler = mock(Scheduler.class);
        SchedulerManager manager = new SchedulerManager(scheduler);

        manager.addJob(job(1L, 1));

        ArgumentCaptor<CronTrigger> trigger = ArgumentCaptor.forClass(CronTrigger.class);
        verify(scheduler).scheduleJob(any(JobDetail.class), trigger.capture());
        assertEquals(MISFIRE_INSTRUCTION_FIRE_ONCE_NOW,
            trigger.getValue().getMisfireInstruction());
    }

    @Test
    void addJobAppliesIgnoreMisfiresPolicy() throws Exception {
        Scheduler scheduler = mock(Scheduler.class);
        SchedulerManager manager = new SchedulerManager(scheduler);

        manager.addJob(job(2L, 0));

        ArgumentCaptor<CronTrigger> trigger = ArgumentCaptor.forClass(CronTrigger.class);
        verify(scheduler).scheduleJob(any(JobDetail.class), trigger.capture());
        assertEquals(MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY,
            trigger.getValue().getMisfireInstruction());
    }

    private SysJob job(Long id, Integer misfirePolicy) {
        SysJob job = new SysJob();
        job.setId(id);
        job.setJobName("demo");
        job.setJobGroup("SYSTEM");
        job.setCronExpression("0 0 0 * * ?");
        job.setHandlerName("demoJob");
        job.setMisfirePolicy(misfirePolicy);
        job.setStatus(1);
        return job;
    }
}
