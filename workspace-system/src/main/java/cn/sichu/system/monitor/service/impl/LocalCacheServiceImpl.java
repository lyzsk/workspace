package cn.sichu.system.monitor.service.impl;

import cn.sichu.system.monitor.service.ICacheService;

import cn.sichu.system.monitor.model.vo.CacheEntryVO;
import cn.sichu.system.monitor.model.vo.CacheStatsVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Service
public class LocalCacheServiceImpl implements ICacheService {

    private final Map<String, CacheEntryVO> entries = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    public CacheEntryVO get(String key) {
        CacheEntryVO entry = entries.get(key);
        if (entry == null) {
            misses.incrementAndGet();
        } else {
            hits.incrementAndGet();
        }
        return entry;
    }

    public void put(String key, String value) {
        entries.put(key, new CacheEntryVO(key, value, LocalDateTime.now()));
    }

    public void delete(String key) {
        entries.remove(key);
    }

    public void clear() {
        entries.clear();
    }

    public CacheStatsVO stats() {
        long hit = hits.get();
        long miss = misses.get();
        long total = hit + miss;
        double rate = total == 0 ? 0.0 : (double)hit / total;
        return new CacheStatsVO((long)entries.size(), hit, miss, rate);
    }

    public List<CacheEntryVO> list() {
        List<CacheEntryVO> result = new ArrayList<>(entries.values());
        result.sort(Comparator.comparing(CacheEntryVO::key));
        return result;
    }
}
