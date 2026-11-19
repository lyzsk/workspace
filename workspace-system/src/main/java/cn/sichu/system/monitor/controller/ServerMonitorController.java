package cn.sichu.system.monitor.controller;

import cn.sichu.common.core.result.Result;
import cn.sichu.system.monitor.model.vo.ServerInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.*;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@RestController
@RequestMapping("/api/monitor/server")
@RequiredArgsConstructor
public class ServerMonitorController {

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:monitor:server')")
    public Result<ServerInfoVO> info() throws Exception {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();
        long totalMb = heap.getCommitted() / 1024 / 1024;
        long usedMb = heap.getUsed() / 1024 / 1024;
        return Result.success(
            new ServerInfoVO(InetAddress.getLocalHost().getHostName(), os.getName(),
                os.getVersion(), System.getProperty("java.version"),
                System.getProperty("java.vendor"), os.getAvailableProcessors(), totalMb, usedMb,
                totalMb - usedMb, heap.getMax() / 1024 / 1024, nonHeap.getUsed() / 1024 / 1024,
                runtime.getUptime() / 1000,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    }
}
