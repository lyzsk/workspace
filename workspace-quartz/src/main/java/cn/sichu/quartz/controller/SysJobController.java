package cn.sichu.quartz.controller;

import cn.sichu.common.log.annotation.Log;
import cn.sichu.common.result.PageResult;
import cn.sichu.common.result.Result;
import cn.sichu.quartz.model.form.SysJobForm;
import cn.sichu.quartz.model.query.SysJobQuery;
import cn.sichu.quartz.model.vo.SysJobVO;
import cn.sichu.quartz.service.IJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@RestController
@RequestMapping("/api/system/jobs")
@RequiredArgsConstructor
public class SysJobController {

    private final IJobService jobService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:job:list')")
    public Result<PageResult<SysJobVO>> page(@Valid SysJobQuery query) {
        return Result.success(jobService.page(query));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:job:create')")
    @Log(module = "定时任务", operation = "新增任务")
    public Result<String> create(@RequestBody @Valid SysJobForm form) {
        return Result.success(jobService.create(form).toString());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:job:update')")
    @Log(module = "定时任务", operation = "修改任务")
    public Result<Void> update(@PathVariable Long id,
        @RequestBody @Valid SysJobForm form) {
        jobService.update(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:job:delete')")
    @Log(module = "定时任务", operation = "删除任务")
    public Result<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/pause")
    @PreAuthorize("@ss.hasPerm('sys:job:update')")
    @Log(module = "定时任务", operation = "暂停任务")
    public Result<Void> pause(@PathVariable Long id) {
        jobService.pause(id);
        return Result.success();
    }

    @PutMapping("/{id}/resume")
    @PreAuthorize("@ss.hasPerm('sys:job:update')")
    @Log(module = "定时任务", operation = "恢复任务")
    public Result<Void> resume(@PathVariable Long id) {
        jobService.resume(id);
        return Result.success();
    }

    @PostMapping("/{id}/run")
    @PreAuthorize("@ss.hasPerm('sys:job:run')")
    @Log(module = "定时任务", operation = "执行一次")
    public Result<Void> run(@PathVariable Long id) {
        jobService.runNow(id);
        return Result.success();
    }
}
