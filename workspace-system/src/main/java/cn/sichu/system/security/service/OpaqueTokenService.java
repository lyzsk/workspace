package cn.sichu.system.security.service;

import cn.sichu.system.auth.model.vo.IntrospectionVO;
import cn.sichu.system.security.entity.SysRefreshToken;
import cn.sichu.system.security.entity.SysUserSession;
import cn.sichu.system.security.mapper.SysRefreshTokenMapper;
import cn.sichu.system.security.mapper.SysUserSessionMapper;
import cn.sichu.system.security.model.AccessTokenState;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.model.SessionState;
import cn.sichu.system.security.token.*;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Service
@RequiredArgsConstructor
public class OpaqueTokenService {

    private final SysUserSessionMapper sessionMapper;
    private final SysRefreshTokenMapper refreshTokenMapper;
    private final SysUserMapper userMapper;
    private final AuthTokenStore authTokenStore;
    private final AuthenticationStateStore stateStore;
    private final SessionActivityRecorder activityRecorder;
    private final OpaqueTokenGenerator tokenGenerator;
    private final OpaqueAuthenticationProperties authenticationProperties;
    private final SessionProperties sessionProperties;
    private final Clock clock;

    @Transactional
    public IssuedTokens createSession(LoginUser user) {
        Instant now = clock.instant();
        Instant sessionExpiresAt =
            now.plus(sessionProperties.getAbsoluteTimeout());
        SysUserSession session = new SysUserSession();
        session.setTenantId(user.getTenantId());
        session.setUserId(user.getUserId());
        session.setSessionCode(tokenGenerator.generate());
        session.setJti(session.getSessionCode());
        session.setRefreshFamily(tokenGenerator.generate());
        session.setTokenVersion(user.getTokenVersion());
        session.setLoginTime(local(now));
        session.setLastSeenAt(local(now));
        session.setExpiresAt(local(sessionExpiresAt));
        session.setRevoked(0);
        session.setIp(cn.sichu.system.security.util.WebRequestUtils.clientIp());
        session.setUserAgent(
            cn.sichu.system.security.util.WebRequestUtils.userAgent());
        session.setIp(
            cn.sichu.common.utils.TextUtils.truncate(session.getIp(), 64));
        session.setUserAgent(
            cn.sichu.common.utils.TextUtils.truncate(session.getUserAgent(),
                500));
        sessionMapper.insert(session);

        String refreshToken = tokenGenerator.generate();
        saveRefreshToken(session.getId(), refreshToken, now, sessionExpiresAt);
        stateStore.saveSession(
            new SessionState(session.getSessionCode(), user.getUserId(),
                user.getTokenVersion(), sessionExpiresAt, now, false, null));
        String accessToken =
            issueAccessToken(session, user.getUserId(), user.getUsername(),
                user.getTenantId(), user.getTokenVersion(), now);
        return new IssuedTokens(accessToken, refreshToken, user.getUserId(),
            accessExpiresAt(now, sessionExpiresAt), sessionExpiresAt);
    }

    @Transactional(noRollbackFor = TokenException.class)
    public IssuedTokens refresh(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new TokenException("refresh token 无效");
        }
        String refreshHash = tokenGenerator.hash(rawRefreshToken);
        SysRefreshToken current = refreshTokenMapper.selectByHash(refreshHash);
        if (current == null) {
            throw new TokenException("refresh token 无效");
        }
        SysUserSession session =
            sessionMapper.selectForUpdateById(current.getSessionId());
        current = refreshTokenMapper.selectForUpdateByHash(refreshHash);
        if (current == null) {
            throw new TokenException("refresh token 无效");
        }
        if (current.getConsumedAt() != null || current.getRevokedAt() != null) {
            authTokenStore.revokeSessionById(current.getSessionId());
            throw new TokenException("refresh token 已轮换或撤销");
        }

        Instant now = clock.instant();
        SessionState runtime = session == null ? null :
            stateStore.findSession(session.getSessionCode()).orElse(null);
        if (!isRefreshActive(current, session, runtime, now)) {
            throw new TokenException("登录会话无效或已过期");
        }
        SysUser user = userMapper.selectById(session.getUserId());
        if (!isUserActive(user, session.getTokenVersion(), now)) {
            throw new TokenException("登录凭证无效");
        }

        String nextRefreshToken = tokenGenerator.generate();
        String nextHash = tokenGenerator.hash(nextRefreshToken);
        if (refreshTokenMapper.consume(current.getId(), local(now), nextHash)
            != 1) {
            authTokenStore.revokeSessionById(session.getId());
            throw new TokenException("refresh token 已轮换或撤销");
        }
        Instant sessionExpiresAt =
            session.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant();
        saveRefreshToken(session.getId(), nextRefreshToken, now,
            sessionExpiresAt);
        session.setLastSeenAt(local(now));
        sessionMapper.updateById(session);
        stateStore.saveSession(
            new SessionState(session.getSessionCode(), user.getId(),
                session.getTokenVersion(), sessionExpiresAt, now, false, null));
        String accessToken =
            issueAccessToken(session, user.getId(), user.getUsername(),
                user.getTenantId(), session.getTokenVersion(), now);
        return new IssuedTokens(accessToken, nextRefreshToken, user.getId(),
            accessExpiresAt(now, sessionExpiresAt), sessionExpiresAt);
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        Set<Long> sessionIds = new LinkedHashSet<>();
        if (StringUtils.hasText(accessToken)) {
            stateStore.findAccessToken(tokenGenerator.hash(accessToken))
                .ifPresent(state -> sessionIds.add(state.sessionId()));
        }
        if (StringUtils.hasText(refreshToken)) {
            SysRefreshToken stored = refreshTokenMapper.selectByHash(
                tokenGenerator.hash(refreshToken));
            if (stored != null) {
                sessionIds.add(stored.getSessionId());
            }
        }
        sessionIds.forEach(authTokenStore::revokeSessionById);
    }

    public IntrospectionVO introspect(String rawAccessToken) {
        if (!StringUtils.hasText(rawAccessToken)) {
            return IntrospectionVO.inactive();
        }
        AccessTokenState access =
            stateStore.findAccessToken(tokenGenerator.hash(rawAccessToken))
                .orElse(null);
        Instant now = clock.instant();
        if (access == null || !now.isBefore(access.expiresAt())) {
            return IntrospectionVO.inactive();
        }
        SessionState session =
            stateStore.findSession(access.sessionCode()).orElse(null);
        if (!isRuntimeActive(session, access, now)) {
            return IntrospectionVO.inactive();
        }
        SysUser user = userMapper.selectById(access.userId());
        if (!isUserActive(user, access.tokenVersion(), now)) {
            return IntrospectionVO.inactive();
        }
        if (!stateStore.touchSession(session.sessionCode(), now)) {
            return IntrospectionVO.inactive();
        }
        activityRecorder.record(session.sessionCode());
        return new IntrospectionVO(true, user.getId(), user.getUsername(),
            user.getTenantId(), access.sessionId(), access.tokenVersion(),
            access.expiresAt());
    }

    public void validateGatewaySession(Long userId, Long sessionId,
        Long tokenVersion) {
        SysUserSession session =
            sessionId == null ? null : sessionMapper.selectById(sessionId);
        Instant now = clock.instant();
        SessionState runtime =
            session == null || session.getSessionCode() == null ? null :
                stateStore.findSession(session.getSessionCode()).orElse(null);
        if (session == null || runtime == null || session.getRevoked() == null
            || session.getRevoked() != 0 || runtime.revoked()
            || !Objects.equals(session.getUserId(), userId) || !Objects.equals(
            session.getTokenVersion(), tokenVersion) || !Objects.equals(
            runtime.userId(), userId) || !Objects.equals(runtime.tokenVersion(),
            tokenVersion) || !now.isBefore(
            session.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant())
            || !now.isBefore(runtime.expiresAt()) || !now.isBefore(
            runtime.lastSeenAt().plus(sessionProperties.getIdleTimeout()))) {
            throw new TokenException("Gateway 身份与会话不匹配");
        }
    }

    private void saveRefreshToken(Long sessionId, String rawToken,
        Instant issuedAt, Instant expiresAt) {
        SysRefreshToken refreshToken = new SysRefreshToken();
        refreshToken.setSessionId(sessionId);
        refreshToken.setTokenHash(tokenGenerator.hash(rawToken));
        refreshToken.setIssuedAt(local(issuedAt));
        refreshToken.setExpiresAt(local(expiresAt));
        refreshTokenMapper.insert(refreshToken);
    }

    private String issueAccessToken(SysUserSession session, Long userId,
        String username, Long tenantId, Long tokenVersion, Instant now) {
        Instant sessionExpiresAt =
            session.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant();
        String accessToken = tokenGenerator.generate();
        AccessTokenState state =
            new AccessTokenState(userId, username, tenantId, session.getId(),
                session.getSessionCode(), tokenVersion, now,
                accessExpiresAt(now, sessionExpiresAt));
        stateStore.saveAccessToken(tokenGenerator.hash(accessToken), state);
        return accessToken;
    }

    private Instant accessExpiresAt(Instant now, Instant sessionExpiresAt) {
        Instant accessExpiresAt =
            now.plus(authenticationProperties.getAccessTokenTtl());
        return accessExpiresAt.isBefore(sessionExpiresAt) ? accessExpiresAt :
            sessionExpiresAt;
    }

    private boolean isRefreshActive(SysRefreshToken token,
        SysUserSession session, SessionState runtime, Instant now) {
        return session != null && runtime != null && !runtime.revoked()
            && session.getRevoked() != null && session.getRevoked() == 0
            && now.isBefore(
            token.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant())
            && now.isBefore(
            session.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant())
            && now.isBefore(runtime.expiresAt()) && now.isBefore(
            runtime.lastSeenAt().plus(sessionProperties.getIdleTimeout()))
            && Objects.equals(session.getUserId(), runtime.userId())
            && Objects.equals(session.getSessionCode(), runtime.sessionCode())
            && Objects.equals(session.getTokenVersion(),
            runtime.tokenVersion());
    }

    private boolean isRuntimeActive(SessionState session,
        AccessTokenState access, Instant now) {
        return session != null && !session.revoked() && Objects.equals(
            session.userId(), access.userId()) && Objects.equals(
            session.tokenVersion(), access.tokenVersion()) && now.isBefore(
            session.expiresAt()) && now.isBefore(
            session.lastSeenAt().plus(sessionProperties.getIdleTimeout()));
    }

    private boolean isUserActive(SysUser user, Long tokenVersion, Instant now) {
        if (user == null || user.getStatus() == null || user.getStatus() != 1
            || !Objects.equals(tokenVersion,
            user.getTokenVersion() == null ? 0L : user.getTokenVersion())) {
            return false;
        }
        return user.getLockedUntil() == null || user.getLockedUntil()
            .atZone(ZoneId.systemDefault()).toInstant().isBefore(now);
    }

    private LocalDateTime local(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
