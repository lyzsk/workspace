package cn.sichu.system.log.controller;

import cn.sichu.common.core.result.PageResult;
import cn.sichu.common.core.result.Result;
import cn.sichu.common.core.utils.CsvUtils;
import cn.sichu.common.core.utils.DateTimeUtils;
import cn.sichu.common.core.utils.ExcelUtils;
import cn.sichu.system.log.model.query.SysOperLogQuery;
import cn.sichu.system.log.model.vo.SysOperLogVO;
import cn.sichu.system.log.service.ILogService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@RestController
@RequestMapping("/api/system/oper-logs")
@RequiredArgsConstructor
public class SysOperLogController {

    private final ILogService logService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:log:oper:list')")
    public Result<PageResult<SysOperLogVO>> page(@Valid SysOperLogQuery query) {
        return Result.success(logService.pageOperLogs(query));
    }

    @GetMapping("/export")
    @PreAuthorize("@ss.hasPerm('sys:log:oper:list')")
    public void export(@Valid SysOperLogQuery query,
        @RequestParam(defaultValue = "xlsx") String format, HttpServletResponse response)
        throws IOException {
        List<String> headers = List.of("模块", "操作", "用户", "部门", "调用方法", "请求地址",
            "Query 参数", "请求参数", "返回结果", "状态", "耗时(ms)", "IP", "User-Agent", "操作时间",
            "异常类型", "错误信息");
        List<List<String>> rows = logService.listOperLogs(query).stream().map(
            log -> List.of(safe(log.getModule()), safe(log.getOperation()), safe(log.getUsername()),
                safe(log.getDeptName()), safe(log.getMethod()), safe(log.getRequestUri()),
                safe(log.getRequestQuery()), safe(log.getRequestParams()),
                safe(log.getResponseResult()),
                log.getStatus() != null && log.getStatus() == 1 ? "成功" : "失败",
                log.getCostTime() == null ? "" : log.getCostTime().toString(), safe(log.getIp()),
                safe(log.getUserAgent()), DateTimeUtils.format(log.getOperTime()),
                safe(log.getErrorType()), safe(log.getErrorMsg()))).toList();
        if ("csv".equalsIgnoreCase(format)) {
            CsvUtils.write(response, "oper-logs.csv", headers, rows);
        } else {
            ExcelUtils.write(response, "oper-logs.xlsx", "操作日志", headers, rows);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
