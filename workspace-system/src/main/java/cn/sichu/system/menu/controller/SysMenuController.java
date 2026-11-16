package cn.sichu.system.menu.controller;

import cn.sichu.common.result.Result;
import cn.sichu.common.log.annotation.Log;
import cn.sichu.system.menu.model.form.SysMenuForm;
import cn.sichu.system.menu.model.vo.SysMenuTreeVO;
import cn.sichu.system.menu.service.IMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final IMenuService menuService;

    @GetMapping("/my")
    public Result<List<SysMenuTreeVO>> myMenus() {
        return Result.success(menuService.currentUserTree());
    }

    @GetMapping("/tree")
    @PreAuthorize("@ss.hasPerm('sys:menu:list') or @ss.hasPerm('sys:role:assign') or @ss.hasPerm('sys:role:update')")
    public Result<List<SysMenuTreeVO>> tree(
        @RequestParam(defaultValue = "false") boolean includeButtons) {
        return Result.success(menuService.tree(includeButtons));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:menu:create')")
    @Log(module = "菜单管理", operation = "新增菜单")
    public Result<String> create(@RequestBody @Valid SysMenuForm form) {
        return Result.success(menuService.create(form).toString());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:menu:update')")
    @Log(module = "菜单管理", operation = "修改菜单")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid SysMenuForm form) {
        menuService.update(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:menu:delete')")
    @Log(module = "菜单管理", operation = "删除菜单")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.success();
    }
}
