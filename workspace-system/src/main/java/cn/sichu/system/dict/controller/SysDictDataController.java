package cn.sichu.system.dict.controller;

import cn.sichu.common.core.result.PageResult;
import cn.sichu.common.core.result.Result;
import cn.sichu.common.core.utils.CsvUtils;
import cn.sichu.common.core.utils.ExcelUtils;
import cn.sichu.system.dict.model.form.SysDictDataForm;
import cn.sichu.system.dict.model.query.SysDictDataQuery;
import cn.sichu.system.dict.model.vo.SysDictDataVO;
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
@RequestMapping("/api/system/dict-data")
@RequiredArgsConstructor
public class SysDictDataController {

    private final IDictService dictService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:dict:list')")
    public Result<PageResult<SysDictDataVO>> page(@Valid SysDictDataQuery query) {
        return Result.success(dictService.pageData(query));
    }

    @GetMapping("/type/{dictType}")
    public Result<List<SysDictDataVO>> listByType(@PathVariable String dictType) {
        return Result.success(dictService.listDataByType(dictType));
    }

    @GetMapping("/export")
    @PreAuthorize("@ss.hasPerm('sys:dict:list')")
    public void export(@Valid SysDictDataQuery query,
        @RequestParam(defaultValue = "xlsx") String format, HttpServletResponse response)
        throws IOException {
        List<String> headers = List.of("字典类型", "显示文本", "字典值", "排序", "状态");
        List<List<String>> rows = dictService.listDataForExport(query).stream().map(
            data -> List.of(safe(data.getDictType()), safe(data.getLabel()), safe(data.getValue()),
                data.getSortNo() == null ? "" : data.getSortNo().toString(),
                data.getStatus() != null && data.getStatus() == 1 ? "启用" : "停用")).toList();
        if ("csv".equalsIgnoreCase(format)) {
            CsvUtils.write(response, "dict-data.csv", headers, rows);
        } else {
            ExcelUtils.write(response, "dict-data.xlsx", "字典数据", headers, rows);
        }
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:dict:create')")
    @Log(module = "字典管理", operation = "新增字典数据")
    public Result<String> create(@RequestBody @Valid SysDictDataForm form) {
        return Result.success(dictService.createData(form).toString());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:dict:update')")
    @Log(module = "字典管理", operation = "修改字典数据")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid SysDictDataForm form) {
        dictService.updateData(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:dict:delete')")
    @Log(module = "字典管理", operation = "删除字典数据")
    public Result<Void> delete(@PathVariable Long id) {
        dictService.deleteData(id);
        return Result.success();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
