package cn.sichu.system.app.grant.controller;

import cn.sichu.common.log.annotation.Log;
import cn.sichu.common.result.Result;
import cn.sichu.system.app.grant.model.form.AppGrantForm;
import cn.sichu.system.app.grant.model.vo.AppGrantVO;
import cn.sichu.system.app.grant.service.IAppGrantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@RestController
@RequestMapping("/api/system/users/{userId}")
@RequiredArgsConstructor
public class UserAppGrantController {

    private final IAppGrantService grantService;

    @GetMapping("/app-grants")
    @PreAuthorize("@ss.hasPerm('sys:user:app-grant')")
    public Result<List<AppGrantVO>> list(@PathVariable Long userId) {
        return Result.success(grantService.listUserGrants(userId));
    }

    @PutMapping("/apps/{appCode}/grant")
    @PreAuthorize("@ss.hasPerm('sys:user:app-grant')")
    @Log(module = "用户管理", operation = "配置应用授权")
    public Result<Void> save(@PathVariable Long userId,
        @PathVariable String appCode, @RequestBody @Valid AppGrantForm form) {
        grantService.saveGrant(userId, appCode, form);
        return Result.success();
    }
}
