package cn.sichu.system.auth.service;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public interface ILoginLogService {

    void record(String username, boolean success, String message, Long userId);

    int increaseLoginFailure(String username);

    void resetLoginFailure(Long userId);
}
