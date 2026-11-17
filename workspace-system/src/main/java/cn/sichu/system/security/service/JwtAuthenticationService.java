package cn.sichu.system.security.service;

import cn.sichu.system.security.entity.SysUserSession;
import cn.sichu.system.security.mapper.SysUserSessionMapper;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.token.JwtTokenManager;
import cn.sichu.system.security.token.SessionProperties;
import cn.sichu.system.security.token.TokenException;
import cn.sichu.system.security.token.TokenPayload;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Service
@RequiredArgsConstructor
public class JwtAuthenticationService {

    private final JwtTokenManager tokenManager;
    private final UserDetailsService userDetailsService;
    private final AuthTokenStore authTokenStore;
    private final SessionActivityRecorder activityRecorder;
    private final SysUserSessionMapper sessionMapper;
    private final SessionProperties sessionProperties;

    public LoginUser authenticate(String token) {
        TokenPayload payload = tokenManager.parseAccessToken(token);
        SysUserSession session = sessionMapper.selectOne(new LambdaQueryWrapper<SysUserSession>()
            .eq(SysUserSession::getJti, payload.tokenId()));
        if (!isSessionActive(session, payload, LocalDateTime.now())) {
            throw new TokenException("登录会话无效或已过期");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(payload.username());
        if (userDetails instanceof LoginUser loginUser
            && loginUser.isEnabled()
            && loginUser.isAccountNonLocked()
            && Objects.equals(payload.tokenVersion(), loginUser.getTokenVersion())
            && !authTokenStore.isAccessTokenRevoked(payload.tokenId())) {
            activityRecorder.record(payload.tokenId());
            return loginUser;
        }
        throw new TokenException("登录凭证无效");
    }

    private boolean isSessionActive(SysUserSession session, TokenPayload payload,
        LocalDateTime now) {
        return session != null
            && Objects.equals(session.getUserId(), payload.userId())
            && session.getRevoked() != null
            && session.getRevoked() == 0
            && session.getExpiresAt() != null
            && now.isBefore(session.getExpiresAt())
            && session.getLastSeenAt() != null
            && now.isBefore(session.getLastSeenAt().plus(sessionProperties.getIdleTimeout()));
    }
}
