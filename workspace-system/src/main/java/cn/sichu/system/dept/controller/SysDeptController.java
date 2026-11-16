package cn.sichu.system.dept.controller;

import cn.sichu.common.result.Result;
import cn.sichu.system.dept.model.form.SysDeptForm;
import cn.sichu.system.dept.model.vo.SysDeptTreeVO;
import cn.sichu.system.dept.service.IDeptService;
import cn.sichu.common.log.annotation.Log;
import jakarta.validation.Valid;
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
@RequestMapping("/api/system/depts")
@RequiredArgsConstructor
public class SysDeptController {

    private final IDeptService deptService;

    @GetMapping("/tree")
    @PreAuthorize("@ss.hasPerm('sys:dept:list')")
    public Result<List<SysDeptTreeVO>> tree() {
        return Result.success(deptService.tree());
    }

    @GetMapping("/list")
    public Result<List<SysDeptTreeVO>> list() {
        return Result.success(deptService.tree());
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:dept:create')")
    @Log(module = "部门管理", operation = "新增部门")
    public Result<String> create(@RequestBody @Valid SysDeptForm form) {
        return Result.success(deptService.create(form).toString());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:dept:update')")
    @Log(module = "部门管理", operation = "修改部门")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid SysDeptForm form) {
        deptService.update(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:dept:delete')")
    @Log(module = "部门管理", operation = "删除部门")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.success();
    }
}
