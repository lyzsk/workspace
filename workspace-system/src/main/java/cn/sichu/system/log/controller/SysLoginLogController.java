package cn.sichu.system.log.controller;

import cn.sichu.common.result.PageResult;
import cn.sichu.common.result.Result;
import cn.sichu.common.utils.CsvUtils;
import cn.sichu.common.utils.DateTimeUtils;
import cn.sichu.common.utils.ExcelUtils;
import cn.sichu.system.log.model.query.SysLoginLogQuery;
import cn.sichu.system.log.model.vo.SysLoginLogVO;
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
@RequestMapping("/api/system/login-logs")
@RequiredArgsConstructor
public class SysLoginLogController {

    private final ILogService logService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:log:login:list')")
    public Result<PageResult<SysLoginLogVO>> page(@Valid SysLoginLogQuery query) {
        return Result.success(logService.pageLoginLogs(query));
    }

    @GetMapping("/export")
    @PreAuthorize("@ss.hasPerm('sys:log:login:list')")
    public void export(@Valid SysLoginLogQuery query,
        @RequestParam(defaultValue = "xlsx") String format, HttpServletResponse response)
        throws IOException {
        List<String> headers = List.of("用户名", "状态", "信息", "IP", "User-Agent", "登录时间");
        List<List<String>> rows = logService.listLoginLogs(query).stream().map(
            log -> List.of(safe(log.getUsername()),
                log.getStatus() != null && log.getStatus() == 1 ? "成功" : "失败",
                safe(log.getMessage()), safe(log.getIp()), safe(log.getUserAgent()),
                DateTimeUtils.format(log.getLoginTime()))).toList();
        if ("csv".equalsIgnoreCase(format)) {
            CsvUtils.write(response, "login-logs.csv", headers, rows);
        } else {
            ExcelUtils.write(response, "login-logs.xlsx", "登录日志", headers, rows);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
