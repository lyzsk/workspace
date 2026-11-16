package cn.sichu.system.online.controller;

import cn.sichu.common.result.Result;
import cn.sichu.common.log.annotation.Log;
import cn.sichu.system.online.model.vo.SysOnlineUserVO;
import cn.sichu.system.online.service.IOnlineUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@RestController
@RequestMapping("/api/system/online-users")
@RequiredArgsConstructor
public class SysOnlineUserController {

    private final IOnlineUserService onlineUserService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:online:list')")
    public Result<List<SysOnlineUserVO>> list() {
        return Result.success(onlineUserService.listOnline());
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@ss.hasPerm('sys:online:kick')")
    @Log(module = "系统监控", operation = "强制下线")
    public Result<Void> kick(@PathVariable Long userId) {
        onlineUserService.kick(userId);
        return Result.success();
    }
}
