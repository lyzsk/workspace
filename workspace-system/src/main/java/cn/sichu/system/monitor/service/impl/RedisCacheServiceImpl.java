package cn.sichu.system.monitor.service.impl;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.core.result.ResultCode;
import cn.sichu.system.monitor.model.vo.CacheEntryVO;
import cn.sichu.system.monitor.model.vo.CacheStatsVO;
import cn.sichu.system.monitor.service.ICacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
@Service
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements ICacheService {

    private static final int MAX_ENTRIES = 500;
    private static final int MAX_VALUE_LENGTH = 4000;
    private static final List<String> PROTECTED_PREFIXES = List.of("login:user:", "login:lock:");

    private final StringRedisTemplate redisTemplate;

    @Override
    public CacheEntryVO get(String key) {
        return entry(key);
    }

    @Override
    public void put(String key, String value) {
        assertWritable(key);
        redisTemplate.opsForValue().set(key, value == null ? "" : value);
    }

    @Override
    public void delete(String key) {
        assertWritable(key);
        redisTemplate.delete(key);
    }

    @Override
    public void clear() {
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(
            ScanOptions.scanOptions().count(100).build())) {
            cursor.forEachRemaining(key -> {
                if (!isProtected(key)) {
                    keys.add(key);
                }
            });
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    public CacheStatsVO stats() {
        return redisTemplate.execute((RedisCallback<CacheStatsVO>) connection -> {
            Properties stats = connection.info("stats");
            long hits = longValue(stats, "keyspace_hits");
            long misses = longValue(stats, "keyspace_misses");
            long total = hits + misses;
            double hitRate = total == 0 ? 0.0 : (double) hits / total;
            Long size = connection.dbSize();
            return new CacheStatsVO("Redis", size == null ? 0L : size, hits, misses, hitRate);
        });
    }

    @Override
    public List<CacheEntryVO> list() {
        List<CacheEntryVO> result = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(
            ScanOptions.scanOptions().count(100).build())) {
            cursor.forEachRemaining(key -> {
                if (result.size() < MAX_ENTRIES) {
                    result.add(entry(key));
                }
            });
        }
        result.sort(Comparator.comparing(CacheEntryVO::key));
        return result;
    }

    private CacheEntryVO entry(String key) {
        String value = redisTemplate.opsForValue().get(key);
        Long ttl = redisTemplate.getExpire(key);
        return new CacheEntryVO(key, truncate(value), ttl);
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_VALUE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_VALUE_LENGTH);
    }

    private long longValue(Properties properties, String key) {
        String value = properties.getProperty(key);
        return value == null ? 0L : Long.parseLong(value);
    }

    private void assertWritable(String key) {
        if (key == null || key.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "缓存 Key 不能为空");
        }
        if (isProtected(key)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "登录会话不允许通过缓存监控修改");
        }
    }

    private boolean isProtected(String key) {
        return PROTECTED_PREFIXES.stream().anyMatch(key::startsWith);
    }
}
