package cn.sichu.quartz.controller;

import cn.sichu.common.core.result.PageResult;
import cn.sichu.common.core.result.Result;
import cn.sichu.quartz.model.query.SysJobQuery;
import cn.sichu.quartz.model.vo.SysJobLogVO;
import cn.sichu.quartz.service.IJobLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@RestController
@RequestMapping("/api/system/job-logs")
@RequiredArgsConstructor
public class SysJobLogController {

    private final IJobLogService jobLogService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:job:list')")
    public Result<PageResult<SysJobLogVO>> page(@Valid SysJobQuery query) {
        return Result.success(jobLogService.page(query));
    }
}
