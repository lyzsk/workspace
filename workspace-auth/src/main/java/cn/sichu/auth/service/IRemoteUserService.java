package cn.sichu.auth.service;

import cn.sichu.auth.model.dto.LoginCredentialsDTO;
import cn.sichu.auth.model.dto.UserOrgDTO;
import cn.sichu.auth.model.form.PasswordForm;
import cn.sichu.auth.model.form.ProfileForm;
import cn.sichu.auth.model.vo.UserInfoVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@FeignClient(name = "workspace-system")
public interface IRemoteUserService {

    @GetMapping("/api/internal/users/login-info/{username}")
    LoginCredentialsDTO getLoginInfo(@PathVariable("username") String username,
        @RequestHeader("X-Workspace-Service-Token") String serviceToken);

    @GetMapping("/api/internal/users/{userId}/org")
    UserOrgDTO getUserOrg(@PathVariable("userId") Long userId,
        @RequestHeader("X-Workspace-Service-Token") String serviceToken);

    @PutMapping("/api/internal/users/{userId}/profile")
    UserInfoVO updateProfile(@PathVariable("userId") Long userId,
        @RequestBody ProfileForm form,
        @RequestHeader("X-Workspace-Service-Token") String serviceToken);

    @PutMapping("/api/internal/users/{userId}/password")
    void changePassword(@PathVariable("userId") Long userId,
        @RequestBody PasswordForm form,
        @RequestHeader("X-Workspace-Service-Token") String serviceToken);
}
