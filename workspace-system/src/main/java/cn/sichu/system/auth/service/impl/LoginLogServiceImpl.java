package cn.sichu.system.auth.service.impl;

import cn.sichu.common.core.utils.TextUtils;
import cn.sichu.system.auth.entity.SysLoginLog;
import cn.sichu.system.auth.mapper.SysLoginLogMapper;
import cn.sichu.system.auth.service.ILoginLogService;
import cn.sichu.system.security.util.WebRequestUtils;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class LoginLogServiceImpl implements ILoginLogService {

    private static final String LOGIN_TYPE_PASSWORD = "PASSWORD";

    private final SysLoginLogMapper loginLogMapper;
    private final SysUserMapper userMapper;

    @Value("${workspace.login.max-fail-count:5}")
    private int maxFailCount;

    @Value("${workspace.login.lock-minutes:15}")
    private long lockMinutes;

    @Override
    public void record(String username, boolean success, String message, Long userId, String ip,
        String userAgent) {
        SysLoginLog entry = new SysLoginLog();
        entry.setTenantId(0L);
        entry.setUserId(userId);
        entry.setUsername(TextUtils.truncate(username, 50));
        entry.setLoginType(LOGIN_TYPE_PASSWORD);
        entry.setStatus(success ? 1 : 0);
        entry.setMessage(TextUtils.truncate(message, 500));
        entry.setLoginTime(LocalDateTime.now());
        try {
            entry.setIp(TextUtils.truncate(
                ip != null && !ip.isBlank() ? ip : WebRequestUtils.clientIp(), 64));
            entry.setUserAgent(TextUtils.truncate(
                userAgent != null && !userAgent.isBlank() ? userAgent
                    : WebRequestUtils.userAgent(), 500));
        } catch (Exception ignored) {
        }
        try {
            loginLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("写登录日志失败: {}", e.getMessage());
        }
    }

    @Override
    public int increaseLoginFailure(String username) {
        SysUser user = userMapper.selectOne(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            return 0;
        }
        int current = user.getLoginFailCount() == null ? 0 : user.getLoginFailCount();
        if (user.getLockedUntil() != null && user.getLockedUntil().isBefore(LocalDateTime.now())) {
            current = 0;
        }
        int failCount = current + 1;
        LambdaUpdateWrapper<SysUser> update = new LambdaUpdateWrapper<>();
        update.eq(SysUser::getId, user.getId()).set(SysUser::getLoginFailCount, failCount)
            .set(SysUser::getUpdateTime, LocalDateTime.now());
        if (failCount >= maxFailCount) {
            update.set(SysUser::getLockedUntil, LocalDateTime.now().plusMinutes(lockMinutes));
        }
        userMapper.update(null, update);
        return failCount;
    }

    @Override
    public void resetLoginFailure(Long userId) {
        LambdaUpdateWrapper<SysUser> update = new LambdaUpdateWrapper<>();
        update.eq(SysUser::getId, userId).set(SysUser::getLoginFailCount, 0)
            .set(SysUser::getLockedUntil, null)
            .set(SysUser::getUpdateTime, LocalDateTime.now());
        userMapper.update(null, update);
    }
}
