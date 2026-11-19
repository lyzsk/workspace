package cn.sichu.system.app.grant.controller;

import cn.sichu.common.result.Result;
import cn.sichu.system.app.grant.model.vo.AppAuthorizationVO;
import cn.sichu.system.app.grant.service.IAppAuthorizationService;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/auth/me/apps")
@RequiredArgsConstructor
public class CurrentAppAuthorizationController {

    private final IAppAuthorizationService authorizationService;

    @GetMapping("/{appCode}/authorization")
    public Result<AppAuthorizationVO> current(@PathVariable String appCode) {
        return Result.success(
            authorizationService.currentAuthorization(appCode));
    }
}
