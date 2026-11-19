package cn.sichu.auth.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@FeignClient(name = "workspace-system", contextId = "remoteLoginLogService")
public interface IRemoteLoginLogService {

    @PostMapping("/api/internal/users/login-log")
    void record(@RequestParam("username") String username,
        @RequestParam("success") boolean success,
        @RequestParam("message") String message,
        @RequestParam(value = "userId", required = false) Long userId,
        @RequestHeader("X-Workspace-Service-Token") String serviceToken);

    @PostMapping("/api/internal/users/login-failure")
    int increaseLoginFailure(@RequestParam("username") String username,
        @RequestHeader("X-Workspace-Service-Token") String serviceToken);

    @PostMapping("/api/internal/users/login-success")
    void resetLoginFailure(@RequestParam("userId") Long userId,
        @RequestHeader("X-Workspace-Service-Token") String serviceToken);
}
