package cn.sichu.system.role.controller;

import cn.sichu.common.core.result.PageResult;
import cn.sichu.common.core.result.Result;
import cn.sichu.common.core.utils.CsvUtils;
import cn.sichu.common.core.utils.DateTimeUtils;
import cn.sichu.common.core.utils.ExcelUtils;
import cn.sichu.common.core.log.annotation.Log;
import cn.sichu.system.role.model.form.SysRoleForm;
import cn.sichu.system.role.model.form.SysRoleMenuForm;
import cn.sichu.system.role.model.query.SysRoleQuery;
import cn.sichu.system.role.model.vo.SysRoleVO;
import cn.sichu.system.role.service.IRoleService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final IRoleService roleService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:role:list')")
    public Result<PageResult<SysRoleVO>> page(@Valid SysRoleQuery query) {
        return Result.success(roleService.page(query));
    }

    @GetMapping("/list")
    public Result<List<SysRoleVO>> list(@RequestParam(required = false) String appCode) {
        return Result.success(roleService.listActive(appCode));
    }

    @GetMapping("/export")
    @PreAuthorize("@ss.hasPerm('sys:role:list')")
    public void export(@Valid SysRoleQuery query,
        @RequestParam(defaultValue = "xlsx") String format, HttpServletResponse response)
        throws IOException {
        List<String> headers =
            List.of("角色编码", "角色名称", "数据范围", "排序", "状态", "创建时间");
        List<List<String>> rows = roleService.list(query).stream().map(
            role -> List.of(safe(role.getRoleCode()), safe(role.getRoleName()),
                dataScopeLabel(role.getDataScope()),
                role.getRoleSort() == null ? "" : role.getRoleSort().toString(),
                role.getStatus() != null && role.getStatus() == 1 ? "启用" : "停用",
                DateTimeUtils.format(role.getCreateTime()))).toList();
        if ("csv".equalsIgnoreCase(format)) {
            CsvUtils.write(response, "roles.csv", headers, rows);
        } else {
            ExcelUtils.write(response, "roles.xlsx", "角色", headers, rows);
        }
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:role:create')")
    @Log(module = "角色管理", operation = "新增角色")
    public Result<String> create(@RequestBody @Valid SysRoleForm form) {
        return Result.success(roleService.create(form).toString());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:role:update')")
    @Log(module = "角色管理", operation = "修改角色")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid SysRoleForm form) {
        roleService.update(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:role:delete')")
    @Log(module = "角色管理", operation = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/menus")
    @PreAuthorize("@ss.hasPerm('sys:role:update') or @ss.hasPerm('sys:role:assign')")
    public Result<List<Long>> menuIds(@PathVariable Long id) {
        return Result.success(roleService.getMenuIds(id));
    }

    @PutMapping("/{id}/menus")
    @PreAuthorize("@ss.hasPerm('sys:role:assign')")
    @Log(module = "角色管理", operation = "分配权限")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody SysRoleMenuForm form) {
        roleService.assignMenus(id, form.menuIds());
        return Result.success();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String dataScopeLabel(Integer dataScope) {
        return switch (dataScope == null ? 5 : dataScope) {
            case 1 -> "全部数据";
            case 2 -> "指定部门";
            case 3 -> "本部门";
            case 4 -> "本部门及以下";
            default -> "仅本人";
        };
    }
}
