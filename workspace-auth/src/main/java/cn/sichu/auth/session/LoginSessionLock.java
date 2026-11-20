package cn.sichu.auth.session;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.core.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
@Component
@RequiredArgsConstructor
public class LoginSessionLock {

    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(10);
    private static final String UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then "
        + "return redis.call('del', KEYS[1]) else return 0 end";

    private final StringRedisTemplate redisTemplate;

    public <T> T withLock(Long userId, Supplier<T> action) {
        String key = "login:lock:" + userId;
        String owner = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, owner, LOCK_TIMEOUT);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该账号正在登录，请稍后重试");
        }
        try {
            return action.get();
        } finally {
            redisTemplate.execute(new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class), List.of(key),
                owner);
        }
    }
}
