package cn.sichu.system.auth.service.impl;

import cn.sichu.common.exception.BusinessException;
import cn.sichu.common.result.ResultCode;
import cn.sichu.common.utils.TextUtils;
import cn.sichu.system.auth.model.form.LoginForm;
import cn.sichu.system.auth.model.form.PasswordForm;
import cn.sichu.system.auth.model.form.ProfileForm;
import cn.sichu.system.auth.model.vo.LoginVO;
import cn.sichu.system.auth.model.vo.UserInfoVO;
import cn.sichu.system.auth.model.vo.UserOrgVO;
import cn.sichu.system.auth.service.IAuthService;
import cn.sichu.system.auth.service.ILoginLogService;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.security.entity.SysUserSession;
import cn.sichu.system.security.mapper.AuthQueryMapper;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.service.AuthTokenStore;
import cn.sichu.system.security.token.JwtTokenManager;
import cn.sichu.system.security.token.TokenException;
import cn.sichu.system.security.token.TokenPayload;
import cn.sichu.system.security.util.SecurityUtils;
import cn.sichu.system.security.util.WebRequestUtils;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenManager tokenManager;
    private final SysUserMapper userMapper;
    private final AuthQueryMapper authQueryMapper;
    private final AuthTokenStore authTokenStore;
    private final ILoginLogService loginLogService;
    private final PasswordEncoder passwordEncoder;

    @Value("${workspace.login.max-fail-count:5}")
    private int maxFailCount;

    @Value("${workspace.login.lock-minutes:15}")
    private long lockMinutes;

    @Override
    public LoginVO login(LoginForm form) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(form.username(), form.password()));
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            resetLoginFailure(loginUser.getUserId());
            loginLogService.recordSuccess(loginUser);
            return new LoginVO(issueAccessToken(loginUser), toUserInfo(loginUser));
        } catch (LockedException e) {
            loginLogService.recordFailure(form.username(), "账号已锁定");
            throw new BusinessException(SystemCode.ACCOUNT_LOCKED);
        } catch (DisabledException e) {
            loginLogService.recordFailure(form.username(), "账号已停用");
            throw new BusinessException(SystemCode.ACCOUNT_DISABLED);
        } catch (AuthenticationException e) {
            int failCount = increaseLoginFailure(form.username());
            String message = failCount >= maxFailCount
                ? "密码错误次数过多，账号已锁定 " + lockMinutes + " 分钟"
                : "用户名或密码错误";
            loginLogService.recordFailure(form.username(), message);
            throw new BusinessException(SystemCode.LOGIN_FAILED, message);
        }
    }

    @Override
    public void logout(String accessToken) {
        if (accessToken == null) {
            return;
        }
        try {
            TokenPayload payload = tokenManager.parseAccessToken(accessToken);
            authTokenStore.revokeAccessToken(payload.tokenId(), payload.expiresAt());
            authTokenStore.revokeSession(payload.tokenId());
        } catch (TokenException ignored) {
        }
    }

    @Override
    public UserInfoVO currentUser() {
        return toUserInfo(SecurityUtils.getLoginUser());
    }

    @Override
    public UserInfoVO updateProfile(ProfileForm form) {
        LoginUser current = SecurityUtils.getLoginUser();
        SysUser user = userMapper.selectById(current.getUserId());
        if (user == null) {
            throw new BusinessException(SystemCode.USER_NOT_FOUND);
        }
        user.setRealName(form.getRealName());
        user.setGender(form.getGender() == null ? 0 : form.getGender());
        user.setEmail(form.getEmail());
        user.setMobile(form.getMobile());
        if (form.getAvatar() != null) {
            user.setAvatar(form.getAvatar());
        }
        userMapper.updateById(user);
        LoginUser updated = new LoginUser(user, current.getRoles(), current.getPermissions());
        return toUserInfo(updated);
    }

    @Override
    @Transactional
    public void changePassword(PasswordForm form) {
        if (!Objects.equals(form.getNewPassword(), form.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "两次输入的新密码不一致");
        }
        Long userId = SecurityUtils.getLoginUser().getUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(SystemCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.FAILED, "当前密码不正确");
        }
        if (passwordEncoder.matches(form.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "新密码不能与当前密码相同");
        }
        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
        user.setTokenVersion((user.getTokenVersion() == null ? 0L : user.getTokenVersion()) + 1);
        userMapper.updateById(user);
        authTokenStore.revokeAllSessions(userId);
    }

    private String issueAccessToken(LoginUser user) {
        String accessToken = tokenManager.createAccessToken(user.getUserId(), user.getUsername(),
            user.getTenantId(), user.getTokenVersion());
        TokenPayload payload = tokenManager.parseAccessToken(accessToken);

        SysUserSession session = new SysUserSession();
        session.setTenantId(user.getTenantId());
        session.setUserId(user.getUserId());
        session.setJti(payload.tokenId());
        session.setLoginTime(LocalDateTime.now());
        session.setLastSeenAt(LocalDateTime.now());
        session.setExpiresAt(
            LocalDateTime.ofInstant(payload.expiresAt(), ZoneId.systemDefault()));
        session.setRevoked(0);
        session.setIp(TextUtils.truncate(WebRequestUtils.clientIp(), 64));
        session.setUserAgent(TextUtils.truncate(WebRequestUtils.userAgent(), 500));
        authTokenStore.saveSession(session);
        return accessToken;
    }

    private UserInfoVO toUserInfo(LoginUser user) {
        UserOrgVO org = authQueryMapper.selectUserOrg(user.getUserId());
        if (org == null) {
            org = new UserOrgVO(null, null);
        }
        return UserInfoVO.from(user, org);
    }

    private int increaseLoginFailure(String username) {
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
        update.eq(SysUser::getId, user.getId()).set(SysUser::getLoginFailCount, failCount);
        if (failCount >= maxFailCount) {
            update.set(SysUser::getLockedUntil, LocalDateTime.now().plusMinutes(lockMinutes));
        }
        userMapper.update(null, update);
        return failCount;
    }

    private void resetLoginFailure(Long userId) {
        LambdaUpdateWrapper<SysUser> update = new LambdaUpdateWrapper<>();
        update.eq(SysUser::getId, userId)
            .set(SysUser::getLoginFailCount, 0)
            .set(SysUser::getLockedUntil, null);
        userMapper.update(null, update);
    }
}
