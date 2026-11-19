package cn.sichu.auth.controller;

import cn.sichu.auth.model.form.LoginForm;
import cn.sichu.auth.model.form.PasswordForm;
import cn.sichu.auth.model.form.ProfileForm;
import cn.sichu.auth.model.vo.LoginAuthentication;
import cn.sichu.auth.model.vo.LoginVO;
import cn.sichu.auth.model.vo.UserInfoVO;
import cn.sichu.auth.service.IAuthService;
import cn.sichu.common.core.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginForm form) {
        LoginAuthentication authentication = authService.login(form);
        return Result.success(authentication.login());
    }

    @PostMapping("/refresh")
    public Result<LoginVO> refresh(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return Result.success(authService.refresh(bearer(authorization)).login());
    }

    @PostMapping("/logout")
    public Result<Void> logout(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.logout(bearer(authorization));
        return Result.success();
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

    @PostMapping("/touch")
    public Result<Void> touch(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.touch(bearer(authorization));
        return Result.success();
    }

    private String bearer(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ") ?
            authorization.substring(7) : null;
    }
}
