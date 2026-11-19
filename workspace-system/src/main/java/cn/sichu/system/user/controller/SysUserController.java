package cn.sichu.system.user.controller;

import cn.sichu.common.core.result.PageResult;
import cn.sichu.common.core.result.Result;
import cn.sichu.common.core.utils.CsvUtils;
import cn.sichu.common.core.utils.DateTimeUtils;
import cn.sichu.common.core.utils.ExcelUtils;
import cn.sichu.system.user.model.form.SysUserForm;
import cn.sichu.system.user.model.query.SysUserQuery;
import cn.sichu.system.user.model.vo.SysUserPageVO;
import cn.sichu.common.core.log.annotation.Log;
import cn.sichu.system.user.service.IUserService;
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
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class SysUserController {
    private final IUserService userService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:user:list')")
    public Result<PageResult<SysUserPageVO>> page(@Valid SysUserQuery query) {
        return Result.success(userService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:user:list')")
    public Result<SysUserPageVO> get(@PathVariable Long id) {
        return Result.success(userService.get(id));
    }

    @GetMapping("/export")
    @PreAuthorize("@ss.hasPerm('sys:user:list')")
    public void export(@Valid SysUserQuery query,
        @RequestParam(defaultValue = "xlsx") String format, HttpServletResponse response)
        throws IOException {
        List<String> headers = List.of("用户名", "姓名", "性别", "邮箱", "手机号", "部门ID",
            "状态", "创建时间");
        List<List<String>> rows = userService.list(query).stream().map(user -> List.of(
            safe(user.getUsername()),
            safe(user.getRealName()),
            genderLabel(user.getGender()),
            safe(user.getEmail()),
            safe(user.getMobile()),
            safe(user.getDeptId()),
            user.getStatus() != null && user.getStatus() == 1 ? "启用" : "停用",
            DateTimeUtils.format(user.getCreateTime())
        )).toList();
        if ("csv".equalsIgnoreCase(format)) {
            CsvUtils.write(response, "users.csv", headers, rows);
        } else {
            ExcelUtils.write(response, "users.xlsx", "用户", headers, rows);
        }
    }

    @DeleteMapping("/batch")
    @PreAuthorize("@ss.hasPerm('sys:user:delete')")
    @Log(module = "用户管理", operation = "批量删除用户")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        userService.deleteBatch(ids);
        return Result.success();
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:user:create')")
    @Log(module = "用户管理", operation = "新增用户")
    public Result<String> create(@RequestBody @Valid SysUserForm form) {
        Long id = userService.create(form);
        return Result.success(id.toString());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:user:update')")
    @Log(module = "用户管理", operation = "修改用户")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid SysUserForm form) {
        userService.update(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:user:delete')")
    @Log(module = "用户管理", operation = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String genderLabel(Integer gender) {
        return gender == null ? "未知" : gender == 1 ? "男" : gender == 2 ? "女" : "未知";
    }
}
