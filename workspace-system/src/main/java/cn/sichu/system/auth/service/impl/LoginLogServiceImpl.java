package cn.sichu.system.auth.service.impl;

import cn.sichu.common.utils.TextUtils;
import cn.sichu.system.auth.entity.SysLoginLog;
import cn.sichu.system.auth.mapper.SysLoginLogMapper;
import cn.sichu.system.auth.service.ILoginLogService;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.util.WebRequestUtils;
import cn.sichu.common.base.BaseImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl extends BaseImpl<SysLoginLogMapper, SysLoginLog> implements ILoginLogService {

    private static final int STATUS_SUCCESS = 1;
    private static final int STATUS_FAILURE = 0;
    private static final String LOGIN_TYPE_PASSWORD = "PASSWORD";

    private final SysLoginLogMapper loginLogMapper;

    @Override
    public void recordSuccess(LoginUser loginUser) {
        SysLoginLog entry = new SysLoginLog();
        entry.setTenantId(loginUser.getTenantId());
        entry.setUserId(loginUser.getUserId());
        entry.setUsername(loginUser.getUsername());
        entry.setLoginType(LOGIN_TYPE_PASSWORD);
        entry.setStatus(STATUS_SUCCESS);
        entry.setMessage("登录成功");
        fillRequest(entry);
        insertLog(entry);
    }

    @Override
    public void recordFailure(String username, String message) {
        SysLoginLog entry = new SysLoginLog();
        entry.setTenantId(0L);
        entry.setUsername(username);
        entry.setLoginType(LOGIN_TYPE_PASSWORD);
        entry.setStatus(STATUS_FAILURE);
        entry.setMessage(message);
        fillRequest(entry);
        insertLog(entry);
    }

    private void fillRequest(SysLoginLog entry) {
        entry.setLoginTime(LocalDateTime.now());
        entry.setIp(TextUtils.truncate(WebRequestUtils.clientIp(), 64));
        entry.setUserAgent(TextUtils.truncate(WebRequestUtils.userAgent(), 500));
    }

    private void insertLog(SysLoginLog entry) {
        try {
            entry.setUsername(TextUtils.truncate(entry.getUsername(), 50));
            entry.setMessage(TextUtils.truncate(entry.getMessage(), 500));
            loginLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("写登录日志失败: {}", e.getMessage());
        }
    }
}
