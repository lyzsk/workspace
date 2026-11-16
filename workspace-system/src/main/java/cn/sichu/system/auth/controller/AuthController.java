package cn.sichu.system.auth.controller;

import cn.sichu.common.result.Result;
import cn.sichu.system.auth.model.form.LoginForm;
import cn.sichu.system.auth.model.form.PasswordForm;
import cn.sichu.system.auth.model.form.ProfileForm;
import cn.sichu.system.auth.model.vo.LoginVO;
import cn.sichu.system.auth.model.vo.UserInfoVO;
import cn.sichu.system.auth.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginForm form) {
        return Result.success(authService.login(form));
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.success(authService.currentUser());
    }

    @PutMapping("/profile")
    public Result<UserInfoVO> updateProfile(@RequestBody @Valid ProfileForm form) {
        return Result.success(authService.updateProfile(form));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody @Valid PasswordForm form) {
        authService.changePassword(form);
        return Result.success();
    }

    @PostMapping("/logout")
    public Result<Void> logout(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String accessToken =
            authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7) : null;
        authService.logout(accessToken);
        return Result.success();
    }
}
