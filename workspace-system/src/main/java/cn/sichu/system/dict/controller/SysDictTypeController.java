package cn.sichu.system.dict.controller;

import cn.sichu.common.core.result.PageResult;
import cn.sichu.common.core.result.Result;
import cn.sichu.common.core.utils.CsvUtils;
import cn.sichu.common.core.utils.ExcelUtils;
import cn.sichu.system.dict.model.form.SysDictTypeForm;
import cn.sichu.system.dict.model.query.SysDictTypeQuery;
import cn.sichu.system.dict.model.vo.SysDictTypeVO;
import cn.sichu.system.dict.service.IDictService;
import cn.sichu.common.core.log.annotation.Log;
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
@RequestMapping("/api/system/dict-types")
@RequiredArgsConstructor
public class SysDictTypeController {

    private final IDictService dictService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:dict:list')")
    public Result<PageResult<SysDictTypeVO>> page(@Valid SysDictTypeQuery query) {
        return Result.success(dictService.pageTypes(query));
    }

    @GetMapping("/list")
    public Result<List<SysDictTypeVO>> list() {
        return Result.success(dictService.listTypes());
    }

    @GetMapping("/export")
    @PreAuthorize("@ss.hasPerm('sys:dict:list')")
    public void export(@Valid SysDictTypeQuery query,
        @RequestParam(defaultValue = "xlsx") String format, HttpServletResponse response)
        throws IOException {
        List<String> headers = List.of("字典类型", "字典名称", "状态");
        List<List<String>> rows = dictService.listTypesForExport(query).stream().map(
            type -> List.of(safe(type.getDictType()), safe(type.getDictName()),
                type.getStatus() != null && type.getStatus() == 1 ? "启用" : "停用")).toList();
        if ("csv".equalsIgnoreCase(format)) {
            CsvUtils.write(response, "dict-types.csv", headers, rows);
        } else {
            ExcelUtils.write(response, "dict-types.xlsx", "字典类型", headers, rows);
        }
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:dict:create')")
    @Log(module = "字典管理", operation = "新增字典类型")
    public Result<String> create(@RequestBody @Valid SysDictTypeForm form) {
        return Result.success(dictService.createType(form).toString());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:dict:update')")
    @Log(module = "字典管理", operation = "修改字典类型")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid SysDictTypeForm form) {
        dictService.updateType(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:dict:delete')")
    @Log(module = "字典管理", operation = "删除字典类型")
    public Result<Void> delete(@PathVariable Long id) {
        dictService.deleteType(id);
        return Result.success();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
