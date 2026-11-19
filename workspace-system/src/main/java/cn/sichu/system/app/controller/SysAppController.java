package cn.sichu.system.app.controller;

import cn.sichu.common.core.result.Result;
import cn.sichu.system.app.model.form.SysAppForm;
import cn.sichu.system.app.model.vo.SysAppVO;
import cn.sichu.system.app.service.ISysAppService;
import cn.sichu.common.core.log.annotation.Log;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@RestController
@RequestMapping("/api/system/apps")
@RequiredArgsConstructor
public class SysAppController {

    private final ISysAppService appService;

    @GetMapping
    public Result<List<SysAppVO>> list() {
        return Result.success(appService.listAll());
    }

    @GetMapping("/runtime")
    public Result<SysAppVO> runtime(@RequestParam String path) {
        return Result.success(appService.getRuntime(path));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:app:create')")
    @Log(module = "应用管理", operation = "新增应用")
    public Result<String> create(@RequestBody @Valid SysAppForm form) {
        return Result.success(appService.create(form).toString());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:app:update')")
    @Log(module = "应用管理", operation = "修改应用")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid SysAppForm form) {
        appService.update(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:app:delete')")
    @Log(module = "应用管理", operation = "删除应用")
    public Result<Void> delete(@PathVariable Long id) {
        appService.delete(id);
        return Result.success();
    }
}
