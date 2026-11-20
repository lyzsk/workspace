package cn.sichu.system.auth.controller;

import cn.sichu.system.auth.model.vo.LoginCredentialsVO;
import cn.sichu.system.auth.model.vo.UserInfoVO;
import cn.sichu.system.auth.model.vo.UserOrgVO;
import cn.sichu.system.auth.service.ILoginLogService;
import cn.sichu.system.security.mapper.AuthQueryMapper;
import cn.sichu.system.security.service.LoginUserDetailsService;
import cn.sichu.system.security.service.ServiceTokenValidator;
import cn.sichu.system.user.model.form.PasswordForm;
import cn.sichu.system.user.model.form.ProfileForm;
import cn.sichu.system.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class UserLoginInfoController {

    private final LoginUserDetailsService userDetailsService;
    private final AuthQueryMapper authQueryMapper;
    private final ServiceTokenValidator tokenValidator;
    private final IUserService userService;
    private final ILoginLogService loginLogService;

    @GetMapping("/login-info/{username}")
    public LoginCredentialsVO loginInfo(@PathVariable String username,
        @RequestHeader("X-Workspace-Service-Token") String serviceToken) {
        tokenValidator.validate(serviceToken);
        return LoginCredentialsVO.from(
            (cn.sichu.common.security.model.LoginUser)
                userDetailsService.loadUserByUsername(username));
    }

    @GetMapping("/{userId}/login-info")
    public LoginCredentialsVO loginInfoById(@PathVariable Long userId,
        @RequestHeader("X-Workspace-Service-Token") String serviceToken) {
        tokenValidator.validate(serviceToken);
        return LoginCredentialsVO.from(userDetailsService.loadUserById(userId));
    }

    @GetMapping("/{userId}/org")
    public UserOrgVO organization(@PathVariable Long userId,
        @RequestHeader("X-Workspace-Service-Token") String serviceToken) {
        tokenValidator.validate(serviceToken);
        return authQueryMapper.selectUserOrg(userId);
    }

    @PutMapping("/{userId}/profile")
    public UserInfoVO updateProfile(@PathVariable Long userId,
        @RequestBody ProfileForm form,
        @RequestHeader("X-Workspace-Service-Token") String serviceToken) {
        tokenValidator.validate(serviceToken);
        return userService.updateProfile(userId, form);
    }

    @PutMapping("/{userId}/password")
    public void changePassword(@PathVariable Long userId,
        @RequestBody PasswordForm form,
        @RequestHeader("X-Workspace-Service-Token") String serviceToken) {
        tokenValidator.validate(serviceToken);
        userService.changePassword(userId, form);
    }

    @PostMapping("/login-log")
    public void recordLoginLog(@RequestParam String username,
        @RequestParam boolean success,
        @RequestParam String message,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) String ip,
        @RequestParam(required = false) String userAgent,
        @RequestHeader("X-Workspace-Service-Token") String serviceToken) {
        tokenValidator.validate(serviceToken);
        loginLogService.record(username, success, message, userId, ip, userAgent);
    }

    @PostMapping("/login-failure")
    public int increaseLoginFailure(@RequestParam String username,
        @RequestHeader("X-Workspace-Service-Token") String serviceToken) {
        tokenValidator.validate(serviceToken);
        return loginLogService.increaseLoginFailure(username);
    }

    @PostMapping("/login-success")
    public void resetLoginFailure(@RequestParam Long userId,
        @RequestHeader("X-Workspace-Service-Token") String serviceToken) {
        tokenValidator.validate(serviceToken);
        loginLogService.resetLoginFailure(userId);
    }
}
