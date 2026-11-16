package cn.sichu.system.auth.service;

import cn.sichu.system.security.model.LoginUser;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public interface ILoginLogService {

    void recordSuccess(LoginUser loginUser);

    void recordFailure(String username, String message);
}
