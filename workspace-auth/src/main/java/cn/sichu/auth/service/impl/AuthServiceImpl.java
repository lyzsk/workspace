package cn.sichu.auth.service.impl;

import cn.sichu.auth.model.dto.UserOrgDTO;
import cn.sichu.auth.model.form.LoginForm;
import cn.sichu.auth.model.form.PasswordForm;
import cn.sichu.auth.model.form.ProfileForm;
import cn.sichu.auth.model.vo.LoginAuthentication;
import cn.sichu.auth.model.vo.LoginVO;
import cn.sichu.auth.model.vo.UserInfoVO;
import cn.sichu.auth.service.*;
import cn.sichu.auth.session.LoginSessionLock;
import cn.sichu.auth.session.SessionProperties;
import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.core.result.ResultCode;
import cn.sichu.common.license.LicenseState;
import cn.sichu.common.license.LicenseStateStore;
import cn.sichu.common.redis.cache.LoginSessionStore;
import cn.sichu.common.security.model.LoginSession;
import cn.sichu.common.security.model.LoginUser;
import cn.sichu.common.security.token.JwtTokenService;
import cn.sichu.common.security.token.TokenException;
import cn.sichu.common.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final int MAX_FAIL_COUNT = 5;

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokenService;
    private final LoginSessionStore sessionStore;
    private final IRemoteUserService remoteUserService;
    private final IRemoteLoginLogService loginLogService;
    private final RemoteLoginUserDetailsService userDetailsService;
    private final ServiceTokenProvider tokenProvider;
    private final SessionProperties sessionProperties;
    private final LoginSessionLock loginSessionLock;
    private final LicenseStateStore licenseStateStore;

    @Override
    public LoginAuthentication login(LoginForm form) {
        try {
            LoginUser user = (LoginUser)authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(form.username(), form.password()))
                .getPrincipal();
            assertLicenseAllows(user);
            LoginAuthentication authentication = loginSessionLock.withLock(user.getUserId(), () -> {
                if (sessionProperties.isSingleSession()) {
                    sessionStore.deleteByUserId(user.getUserId());
                }
                return issue(user);
            });
            loginLogService.resetLoginFailure(user.getUserId(), tokenProvider.serviceToken());
            recordLogin(user.getUsername(), true, "登录成功", user.getUserId());
            return authentication;
        } catch (LockedException e) {
            recordLogin(form.username(), false, "账号已锁定", null);
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已锁定，请稍后再试");
        } catch (DisabledException e) {
            recordLogin(form.username(), false, "账号已停用", null);
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已停用");
        } catch (AuthenticationException e) {
            int failures =
                loginLogService.increaseLoginFailure(form.username(), tokenProvider.serviceToken());
            String message = failures >= MAX_FAIL_COUNT ? "密码错误次数过多，账号已锁定 15 分钟" :
                "用户名或密码错误";
            recordLogin(form.username(), false, message, null);
            throw new BusinessException(ResultCode.UNAUTHORIZED, message);
        }
    }

    @Override
    public LoginAuthentication refresh(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new TokenException("登录凭证不能为空");
        }
        String sessionId = tokenService.parseIgnoringExpiration(accessToken).sessionId();
        LoginSession session = sessionStore.find(sessionId)
            .orElseThrow(() -> new TokenException("登录会话不存在或已过期"));
        LocalDateTime now = LocalDateTime.now();
        LoginUser user = session.loginUser();
        if (session.revoked() || now.isAfter(session.expiresAt()) || now.isAfter(
            session.lastSeenAt().plus(sessionProperties.getIdleTimeout())) || !user.isEnabled()
            || !user.isAccountNonLocked()) {
            sessionStore.delete(sessionId);
            throw new TokenException("登录会话无效或已过期");
        }
        LoginUser fresh = userDetailsService.loadUserById(user.getUserId());
        // 刷新时把最新权限写回会话，避免 getMe/system 继续读旧 LoginUser。
        sessionStore.save(new LoginSession(sessionId, fresh, session.loginTime(), now,
            session.expiresAt(), session.revoked(), session.revokedAt(), session.ip(),
            session.userAgent()));
        String token = tokenService.createToken(fresh, sessionId);
        return new LoginAuthentication(new LoginVO(token, toUserInfo(fresh)), sessionId,
            session.expiresAt());
    }

    @Override
    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        try {
            sessionStore.delete(tokenService.parseIgnoringExpiration(accessToken).sessionId());
        } catch (TokenException ignored) {
        }
    }

    @Override
    public void touch(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        try {
            sessionStore.touch(tokenService.parse(accessToken).sessionId());
        } catch (TokenException ignored) {
        }
    }

    @Override
    public UserInfoVO currentUser() {
        return toUserInfo(SecurityUtils.getLoginUser());
    }

    @Override
    public UserInfoVO updateProfile(ProfileForm form) {
        Long userId = SecurityUtils.getLoginUser().getUserId();
        return remoteUserService.updateProfile(userId, form, tokenProvider.serviceToken());
    }

    @Override
    public void changePassword(PasswordForm form) {
        Long userId = SecurityUtils.getLoginUser().getUserId();
        remoteUserService.changePassword(userId, form, tokenProvider.serviceToken());
        sessionStore.deleteByUserId(userId);
    }

    private void assertLicenseAllows(LoginUser user) {
        LicenseState state = licenseStateStore.find().orElse(null);
        // 状态缺失或 Redis 不可读时按失效处理；只保留应急管理员的恢复通道。
        boolean blocked = state == null || state.getStatus() == null
            || state.getStatus().isBlocked();
        if (blocked && !isEmergencyAdmin(user)) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE,
                "平台授权已失效，请联系管理员");
        }
    }

    private boolean isEmergencyAdmin(LoginUser user) {
        return user.getRoles() != null && user.getRoles().contains("SUPER_ADMIN");
    }

    private void recordLogin(String username, boolean success, String message, Long userId) {
        ClientRequestMetadata client = ClientRequestMetadata.current();
        loginLogService.record(username, success, message, userId, client.ip(), client.userAgent(),
            tokenProvider.serviceToken());
    }

    private LoginAuthentication issue(LoginUser user) {
        LocalDateTime now = LocalDateTime.now();
        String sessionId = UUID.randomUUID().toString();
        ClientRequestMetadata client = ClientRequestMetadata.current();
        LoginSession session = new LoginSession(sessionId, user, now, now,
            now.plus(sessionProperties.getAbsoluteTimeout()), false, null, client.ip(),
            client.userAgent());
        sessionStore.save(session);
        String accessToken = tokenService.createToken(user, sessionId);
        return new LoginAuthentication(new LoginVO(accessToken, toUserInfo(user)), sessionId,
            session.expiresAt());
    }

    private UserInfoVO toUserInfo(LoginUser user) {
        UserOrgDTO org =
            remoteUserService.getUserOrg(user.getUserId(), tokenProvider.serviceToken());
        return UserInfoVO.from(user, org == null ? null : org.tenantName(),
            org == null ? null : org.deptName());
    }
}
