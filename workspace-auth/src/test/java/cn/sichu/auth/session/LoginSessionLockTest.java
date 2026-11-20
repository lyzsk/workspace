package cn.sichu.auth.session;

import cn.sichu.common.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
class LoginSessionLockTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final LoginSessionLock lock = new LoginSessionLock(redisTemplate);

    @Test
    void releasesOwnedLockAfterLogin() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), eq(Duration.ofSeconds(10))))
            .thenReturn(true);

        String result = lock.withLock(1L, () -> "ok");

        assertEquals("ok", result);
        verify(redisTemplate).execute(any(DefaultRedisScript.class),
            eq(List.of("login:lock:1")), anyString());
    }

    @Test
    void rejectsConcurrentLogin() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), eq(Duration.ofSeconds(10))))
            .thenReturn(false);

        assertThrows(BusinessException.class, () -> lock.withLock(1L, () -> "ok"));
    }
}
