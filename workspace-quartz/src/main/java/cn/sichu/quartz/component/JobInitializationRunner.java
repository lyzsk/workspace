package cn.sichu.quartz.component;

import cn.sichu.quartz.entity.SysJob;
import cn.sichu.quartz.manager.SchedulerManager;
import cn.sichu.quartz.mapper.SysJobMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobInitializationRunner implements CommandLineRunner {

    private final SysJobMapper jobMapper;
    private final SchedulerManager schedulerManager;

    @Override
    public void run(String... args) {
        List<SysJob> jobs = jobMapper.selectList(new LambdaQueryWrapper<>());
        schedulerManager.reconcileWith(jobs);
        log.info("定时任务全量对账完成 count={}", jobs.size());
    }
}
