package cn.sichu.system.monitor.controller;

import cn.sichu.common.result.Result;
import cn.sichu.system.monitor.model.vo.CacheEntryVO;
import cn.sichu.system.monitor.model.vo.CacheStatsVO;
import cn.sichu.system.monitor.service.ICacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@RestController
@RequestMapping("/api/monitor/cache")
@RequiredArgsConstructor
public class CacheMonitorController {

    private final ICacheService cacheService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:monitor:cache')")
    public Result<CacheStatsVO> stats() {
        return Result.success(cacheService.stats());
    }

    @GetMapping("/keys")
    @PreAuthorize("@ss.hasPerm('sys:monitor:cache')")
    public Result<List<CacheEntryVO>> keys() {
        return Result.success(cacheService.list());
    }

    @GetMapping("/{key}")
    @PreAuthorize("@ss.hasPerm('sys:monitor:cache')")
    public Result<CacheEntryVO> get(@PathVariable String key) {
        return Result.success(cacheService.get(key));
    }

    @PutMapping("/{key}")
    @PreAuthorize("@ss.hasPerm('sys:monitor:cache')")
    public Result<Void> put(@PathVariable String key, @RequestBody Map<String, String> body) {
        cacheService.put(key, body.getOrDefault("value", ""));
        return Result.success();
    }

    @DeleteMapping("/{key}")
    @PreAuthorize("@ss.hasPerm('sys:monitor:cache')")
    public Result<Void> delete(@PathVariable String key) {
        cacheService.delete(key);
        return Result.success();
    }

    @DeleteMapping
    @PreAuthorize("@ss.hasPerm('sys:monitor:cache')")
    public Result<Void> clear() {
        cacheService.clear();
        return Result.success();
    }
}
