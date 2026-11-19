package cn.sichu.system.dashboard.controller;

import cn.sichu.common.core.result.Result;
import cn.sichu.system.dashboard.model.vo.DashboardVO;
import cn.sichu.system.dashboard.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
@RestController
@RequestMapping("/api/system/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;

    @PostMapping("/access-events/{appCode}")
    public Result<Void> recordAccess(@PathVariable String appCode) {
        dashboardService.recordAccess(appCode);
        return Result.success();
    }

    @GetMapping
    public Result<DashboardVO> summary() {
        return Result.success(dashboardService.summary());
    }
}
