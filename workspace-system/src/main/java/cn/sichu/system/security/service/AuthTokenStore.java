package cn.sichu.system.security.service;

import cn.sichu.system.security.entity.SysTokenBlacklist;
import cn.sichu.system.security.entity.SysUserSession;
import cn.sichu.system.security.mapper.SysTokenBlacklistMapper;
import cn.sichu.system.security.mapper.SysUserSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenStore {

    private final SysUserSessionMapper sessionMapper;
    private final SysTokenBlacklistMapper tokenBlacklistMapper;
    private final SessionActivityRecorder activityRecorder;

    public void saveSession(SysUserSession session) {
        sessionMapper.insert(session);
    }

    public void revokeSession(String jti) {
        if (jti == null) {
            return;
        }
        activityRecorder.evict(jti);
        LambdaUpdateWrapper<SysUserSession> update = new LambdaUpdateWrapper<>();
        update.eq(SysUserSession::getJti, jti).eq(SysUserSession::getRevoked, 0)
            .set(SysUserSession::getRevoked, 1)
            .set(SysUserSession::getRevokedAt, LocalDateTime.now());
        sessionMapper.update(null, update);
    }

    public void revokeAllSessions(Long userId) {
        LambdaUpdateWrapper<SysUserSession> update = new LambdaUpdateWrapper<>();
        update.eq(SysUserSession::getUserId, userId).eq(SysUserSession::getRevoked, 0)
            .set(SysUserSession::getRevoked, 1)
            .set(SysUserSession::getRevokedAt, LocalDateTime.now());
        sessionMapper.update(null, update);
        activityRecorder.clear();
    }

    public void revokeAccessToken(String jti, Instant expiresAt) {
        if (jti == null || expiresAt == null || expiresAt.isBefore(Instant.now())) {
            return;
        }
        SysTokenBlacklist entry = new SysTokenBlacklist();
        entry.setJti(jti);
        entry.setExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()));
        entry.setCreateTime(LocalDateTime.now());
        try {
            tokenBlacklistMapper.insert(entry);
        } catch (DuplicateKeyException e) {
        }
    }

    public boolean isAccessTokenRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        return tokenBlacklistMapper.selectCount(
            new LambdaQueryWrapper<SysTokenBlacklist>()
                .eq(SysTokenBlacklist::getJti, jti)) > 0;
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanupExpired() {
        LocalDateTime now = LocalDateTime.now();
        int blacklist = tokenBlacklistMapper.delete(
            new LambdaQueryWrapper<SysTokenBlacklist>()
                .lt(SysTokenBlacklist::getExpiresAt, now));
        int session = sessionMapper.delete(
            new LambdaQueryWrapper<SysUserSession>()
                .lt(SysUserSession::getExpiresAt, now.minusDays(1)));
        activityRecorder.clear();
        log.info("token 清理完成 blacklist={} session={}", blacklist, session);
    }
}
