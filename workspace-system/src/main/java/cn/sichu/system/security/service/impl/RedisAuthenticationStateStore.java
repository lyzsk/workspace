package cn.sichu.system.security.service.impl;

import cn.sichu.system.security.model.AccessTokenState;
import cn.sichu.system.security.model.SessionState;
import cn.sichu.system.security.service.AuthenticationStateException;
import cn.sichu.system.security.service.AuthenticationStateStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
public class RedisAuthenticationStateStore implements AuthenticationStateStore {

    private static final String ACCESS_PREFIX = "workspace:auth:access:";
    private static final String SESSION_PREFIX = "workspace:auth:session:";
    private static final DefaultRedisScript<Long> TOUCH_SCRIPT =
        new DefaultRedisScript<>("""
            local value = redis.call('GET', KEYS[1])
            if not value then return 0 end
            local state = cjson.decode(value)
            if state.revoked then return 0 end
            local ttl = redis.call('PTTL', KEYS[1])
            if ttl <= 0 then return 0 end
            state.lastSeenAt = ARGV[1]
            redis.call('SET', KEYS[1], cjson.encode(state), 'PX', ttl)
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> REVOKE_SCRIPT =
        new DefaultRedisScript<>("""
            local value = redis.call('GET', KEYS[1])
            if not value then return 0 end
            local state = cjson.decode(value)
            local ttl = redis.call('PTTL', KEYS[1])
            if ttl <= 0 then return 0 end
            state.revoked = true
            state.revokedAt = ARGV[1]
            redis.call('SET', KEYS[1], cjson.encode(state), 'PX', ttl)
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAuthenticationStateStore(StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveAccessToken(String tokenHash, AccessTokenState state) {
        write(ACCESS_PREFIX + tokenHash, state, state.expiresAt());
    }

    @Override
    public Optional<AccessTokenState> findAccessToken(String tokenHash) {
        return Optional.ofNullable(
            read(ACCESS_PREFIX + tokenHash, AccessTokenState.class));
    }

    @Override
    public void saveSession(SessionState state) {
        write(SESSION_PREFIX + state.sessionCode(), state, state.expiresAt());
    }

    @Override
    public Optional<SessionState> findSession(String sessionCode) {
        return Optional.ofNullable(
            read(SESSION_PREFIX + sessionCode, SessionState.class));
    }

    @Override
    public boolean touchSession(String sessionCode, Instant lastSeenAt) {
        try {
            Long touched = redisTemplate.execute(TOUCH_SCRIPT,
                List.of(SESSION_PREFIX + sessionCode), lastSeenAt.toString());
            return touched != null && touched == 1L;
        } catch (RuntimeException e) {
            throw new AuthenticationStateException("认证状态服务不可用", e);
        }
    }

    @Override
    public void revokeSession(String sessionCode, Instant revokedAt) {
        try {
            redisTemplate.execute(REVOKE_SCRIPT,
                List.of(SESSION_PREFIX + sessionCode), revokedAt.toString());
        } catch (RuntimeException e) {
            throw new AuthenticationStateException("认证状态服务不可用", e);
        }
    }

    private <T> T read(String key, Class<T> type) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? null : objectMapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new AuthenticationStateException("认证状态数据无效", e);
        } catch (RuntimeException e) {
            throw new AuthenticationStateException("认证状态服务不可用", e);
        }
    }

    private void write(String key, Object state, Instant expiresAt) {
        try {
            Duration ttl = Duration.between(Instant.now(), expiresAt);
            if (ttl.isNegative() || ttl.isZero()) {
                redisTemplate.delete(key);
                return;
            }
            redisTemplate.opsForValue()
                .set(key, objectMapper.writeValueAsString(state), ttl);
        } catch (JsonProcessingException e) {
            throw new AuthenticationStateException("认证状态数据无效", e);
        } catch (RuntimeException e) {
            throw new AuthenticationStateException("认证状态服务不可用", e);
        }
    }
}
