package cn.sichu.system.app.grant.controller;

import cn.sichu.common.result.Result;
import cn.sichu.system.app.grant.model.vo.AppAuthorizationVO;
import cn.sichu.system.app.grant.service.IAppAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@RestController
@RequestMapping("/api/internal/authorization/apps")
@RequiredArgsConstructor
public class InternalAppAuthorizationController {

    private final IAppAuthorizationService authorizationService;

    @GetMapping("/{appCode}/users/{userId}")
    public Result<AppAuthorizationVO> authorization(
        @PathVariable String appCode, @PathVariable Long userId,
        Authentication authentication) {
        if (authentication == null || !appCode.equals(
            authentication.getName())) {
            throw new AccessDeniedException("服务凭证与目标应用不匹配");
        }
        return Result.success(
            authorizationService.getAuthorization(userId, appCode));
    }
}
