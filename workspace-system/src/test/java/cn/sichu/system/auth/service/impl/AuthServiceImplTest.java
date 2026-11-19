package cn.sichu.system.auth.service.impl;

import cn.sichu.common.exception.BusinessException;
import cn.sichu.system.auth.model.LoginAuthentication;
import cn.sichu.system.auth.model.form.LoginForm;
import cn.sichu.system.auth.model.form.PasswordForm;
import cn.sichu.system.auth.model.form.ProfileForm;
import cn.sichu.system.auth.model.vo.UserOrgVO;
import cn.sichu.system.auth.service.ILoginLogService;
import cn.sichu.system.code.SystemCode;
import cn.sichu.system.security.mapper.AuthQueryMapper;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.service.AuthTokenStore;
import cn.sichu.system.security.service.OpaqueTokenService;
import cn.sichu.system.security.token.IssuedTokens;
import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
class AuthServiceImplTest {

    private AuthenticationManager authenticationManager;
    private OpaqueTokenService tokenService;
    private SysUserMapper userMapper;
    private AuthQueryMapper authQueryMapper;
    private AuthTokenStore authTokenStore;
    private PasswordEncoder passwordEncoder;
    private ILoginLogService loginLogService;
    private AuthServiceImpl authService;
    private LoginUser loginUser;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            SysUser.class);
    }

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        tokenService = mock(OpaqueTokenService.class);
        userMapper = mock(SysUserMapper.class);
        authQueryMapper = mock(AuthQueryMapper.class);
        authTokenStore = mock(AuthTokenStore.class);
        passwordEncoder = mock(PasswordEncoder.class);
        loginLogService = mock(ILoginLogService.class);
        authService = new AuthServiceImpl(authenticationManager, tokenService, userMapper,
            authQueryMapper, authTokenStore, loginLogService, passwordEncoder);
        ReflectionTestUtils.setField(authService, "maxFailCount", 5);
        ReflectionTestUtils.setField(authService, "lockMinutes", 15L);
        loginUser = loginUser();
    }

    @Test
    void loginReturnsOnlyAccessTokenInLoginVo() {
        when(authenticationManager.authenticate(any())).thenReturn(
            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
        Instant expiresAt = Instant.now().plusSeconds(3600);
        when(tokenService.createSession(loginUser)).thenReturn(
            new IssuedTokens("access-token", "refresh-token", 1L, expiresAt, expiresAt));
        when(userMapper.update(any(), any())).thenReturn(1);

        LoginAuthentication result = authService.login(new LoginForm("admin", "test-password"));

        assertEquals("access-token", result.login().token());
        assertEquals("refresh-token", result.refreshToken());
        verify(tokenService).createSession(loginUser);
        verify(loginLogService).recordSuccess(loginUser);
    }

    @Test
    void refreshReturnsRotatedAccessToken() {
        Instant expiresAt = Instant.now().plusSeconds(3600);
        when(tokenService.refresh("old-refresh")).thenReturn(
            new IssuedTokens("new-access", "new-refresh", 1L, expiresAt, expiresAt));
        when(userMapper.selectById(1L)).thenReturn(user());
        when(authQueryMapper.selectRoleCodes(1L)).thenReturn(List.of("ADMIN"));
        when(authQueryMapper.selectPermissions(1L)).thenReturn(List.of());

        LoginAuthentication result = authService.refresh("old-refresh");

        assertEquals("new-access", result.login().token());
        assertEquals("new-refresh", result.refreshToken());
    }

    @Test
    void loginMapsBadCredentialsToLoginFailed() {
        when(authenticationManager.authenticate(any())).thenThrow(
            new BadCredentialsException("bad credentials"));
        when(userMapper.selectOne(any())).thenReturn(user());
        when(userMapper.update(any(), any())).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> authService.login(new LoginForm("admin", "wrong")));

        assertEquals(SystemCode.LOGIN_FAILED, exception.getResultCode());
        verify(loginLogService).recordFailure("admin", "用户名或密码错误");
    }

    @Test
    void loginMapsDisabledAccount() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> authService.login(new LoginForm("admin", "test-password")));

        assertEquals(SystemCode.ACCOUNT_DISABLED, exception.getResultCode());
    }

    @Test
    void loginMapsLockedAccount() {
        when(authenticationManager.authenticate(any())).thenThrow(new LockedException("locked"));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> authService.login(new LoginForm("admin", "test-password")));

        assertEquals(SystemCode.ACCOUNT_LOCKED, exception.getResultCode());
    }

    @Test
    void logoutDelegatesAccessAndRefreshCredentials() {
        authService.logout("access", "refresh");

        verify(tokenService).logout("access", "refresh");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateProfileReturnsUpdatedOrganizationAndAvatar() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
        SysUser stored = user();
        when(userMapper.selectById(1L)).thenReturn(stored);
        when(authQueryMapper.selectUserOrg(1L)).thenReturn(new UserOrgVO("默认公司", "总公司"));
        ProfileForm form = new ProfileForm();
        form.setRealName("新姓名");
        form.setGender(2);
        form.setEmail("test@example.com");
        form.setMobile("13800000000");
        form.setAvatar("avatar-female");

        var result = authService.updateProfile(form);

        assertEquals("新姓名", result.realName());
        assertEquals("avatar-female", result.avatar());
        assertEquals("总公司", result.deptName());
        verify(userMapper).updateById(stored);
    }

    @Test
    void changePasswordIncrementsVersionAndRevokesSessions() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
        SysUser stored = user();
        when(userMapper.selectById(1L)).thenReturn(stored);
        when(passwordEncoder.matches("current", "hash")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "hash")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
        PasswordForm form = new PasswordForm();
        form.setCurrentPassword("current");
        form.setNewPassword("new-password");
        form.setConfirmPassword("new-password");

        authService.changePassword(form);

        assertEquals("new-hash", stored.getPassword());
        assertEquals(1L, stored.getTokenVersion());
        verify(userMapper).updateById(stored);
        verify(authTokenStore).revokeAllSessions(1L);
    }

    private LoginUser loginUser() {
        return new LoginUser(user(), List.of("ADMIN"), List.of("sys:user:list"));
    }

    private SysUser user() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setTenantId(0L);
        user.setUsername("admin");
        user.setPassword("hash");
        user.setRealName("管理员");
        user.setGender(1);
        user.setStatus(1);
        user.setTokenVersion(0L);
        user.setLoginFailCount(0);
        return user;
    }
}
