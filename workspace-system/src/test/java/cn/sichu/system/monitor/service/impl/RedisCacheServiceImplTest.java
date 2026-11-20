package cn.sichu.system.monitor.service.impl;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.system.monitor.model.vo.CacheStatsVO;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
class RedisCacheServiceImplTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final RedisCacheServiceImpl service = new RedisCacheServiceImpl(redisTemplate);

    @Test
    void readsRedisStatistics() {
        RedisConnection connection = mock(RedisConnection.class);
        Properties properties = new Properties();
        properties.setProperty("keyspace_hits", "8");
        properties.setProperty("keyspace_misses", "2");
        when(connection.info("stats")).thenReturn(properties);
        when(connection.dbSize()).thenReturn(2L);
        when(redisTemplate.execute(any(RedisCallback.class))).thenAnswer(invocation ->
            ((RedisCallback<?>) invocation.getArgument(0)).doInRedis(connection));

        CacheStatsVO result = service.stats();

        assertEquals("Redis", result.engine());
        assertEquals(2L, result.size());
        assertEquals(8L, result.hits());
        assertEquals(2L, result.misses());
        assertEquals(0.8, result.hitRate());
    }

    @Test
    void rejectsDeletingLoginSessionsThroughCacheMonitor() {
        assertThrows(BusinessException.class,
            () -> service.delete("login:user:session-id"));
        assertThrows(BusinessException.class,
            () -> service.put("login:lock:1", "value"));
    }
}
