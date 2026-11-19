package cn.sichu.system.security.service;

import cn.sichu.system.security.entity.SysTokenBlacklist;
import cn.sichu.system.security.entity.SysUserSession;
import cn.sichu.system.security.mapper.SysRefreshTokenMapper;
import cn.sichu.system.security.mapper.SysTokenBlacklistMapper;
import cn.sichu.system.security.mapper.SysUserSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

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
    private final SysRefreshTokenMapper refreshTokenMapper;
    private final SessionActivityRecorder activityRecorder;
    private final AuthenticationStateStore stateStore;

    public void saveSession(SysUserSession session) {
        sessionMapper.insert(session);
    }

    @Transactional
    public void revokeSession(String sessionCode) {
        if (sessionCode == null) {
            return;
        }
        SysUserSession session =
            sessionMapper.selectForUpdateBySessionCode(sessionCode);
        revoke(session);
    }

    @Transactional
    public void revokeSessionById(Long sessionId) {
        if (sessionId != null) {
            revoke(sessionMapper.selectForUpdateById(sessionId));
        }
    }

    @Transactional
    public void revokeAllSessions(Long userId) {
        List<SysUserSession> sessions =
            sessionMapper.selectForUpdateByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<SysUserSession> update =
            new LambdaUpdateWrapper<>();
        update.eq(SysUserSession::getUserId, userId)
            .eq(SysUserSession::getRevoked, 0)
            .set(SysUserSession::getRevoked, 1)
            .set(SysUserSession::getRevokedAt, now);
        sessionMapper.update(null, update);
        for (SysUserSession session : sessions) {
            refreshTokenMapper.revokeBySessionId(session.getId(), now);
            activityRecorder.evict(sessionCode(session));
        }
        stateStore.revokeSessions(
            sessions.stream().map(this::sessionCode).filter(Objects::nonNull)
                .toList(), Instant.now());
    }

    public void revokeAccessToken(String jti, Instant expiresAt) {
        if (jti == null || expiresAt == null || expiresAt.isBefore(
            Instant.now())) {
            return;
        }
        SysTokenBlacklist entry = new SysTokenBlacklist();
        entry.setJti(jti);
        entry.setExpiresAt(
            LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()));
        entry.setCreateTime(LocalDateTime.now());
        try {
            tokenBlacklistMapper.insert(entry);
        } catch (DuplicateKeyException ignored) {
        }
    }

    public boolean isAccessTokenRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        return tokenBlacklistMapper.selectCount(
            new LambdaQueryWrapper<SysTokenBlacklist>().eq(
                SysTokenBlacklist::getJti, jti)) > 0;
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanupExpired() {
        LocalDateTime now = LocalDateTime.now();
        int blacklist = tokenBlacklistMapper.delete(
            new LambdaQueryWrapper<SysTokenBlacklist>().lt(
                SysTokenBlacklist::getExpiresAt, now));
        int session = sessionMapper.delete(
            new LambdaQueryWrapper<SysUserSession>().lt(
                SysUserSession::getExpiresAt, now.minusDays(1)));
        activityRecorder.clear();
        log.info("token 清理完成 blacklist={} session={}", blacklist, session);
    }

    private void revoke(SysUserSession session) {
        if (session == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<SysUserSession> update =
            new LambdaUpdateWrapper<>();
        update.eq(SysUserSession::getId, session.getId())
            .eq(SysUserSession::getRevoked, 0)
            .set(SysUserSession::getRevoked, 1)
            .set(SysUserSession::getRevokedAt, now);
        sessionMapper.update(null, update);
        refreshTokenMapper.revokeBySessionId(session.getId(), now);
        String sessionCode = sessionCode(session);
        activityRecorder.evict(sessionCode);
        if (sessionCode != null) {
            stateStore.revokeSession(sessionCode, Instant.now());
        }
    }

    private String sessionCode(SysUserSession session) {
        return session.getSessionCode() == null ? session.getJti() :
            session.getSessionCode();
    }
}
