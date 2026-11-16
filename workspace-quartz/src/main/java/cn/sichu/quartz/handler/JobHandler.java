package cn.sichu.quartz.handler;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public interface JobHandler {

    void execute(String params);
}
