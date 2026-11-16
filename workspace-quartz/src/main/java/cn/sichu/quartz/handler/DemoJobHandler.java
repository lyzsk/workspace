package cn.sichu.quartz.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Slf4j
@Component("demoJob")
public class DemoJobHandler implements JobHandler {

    @Override
    public void execute(String params) {
        log.info("demoJob 执行时间={} params={}", LocalDateTime.now(), params);
    }
}
