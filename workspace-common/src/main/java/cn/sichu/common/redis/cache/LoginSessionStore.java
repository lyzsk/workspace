package cn.sichu.common.redis.cache;

import cn.sichu.common.security.model.LoginSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
public class LoginSessionStore {

    private static final Duration IDLE_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public LoginSessionStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(LoginSession session) {
        try {
            redisTemplate.opsForValue().set(CacheKeys.loginUser(session.sessionId()),
                objectMapper.writeValueAsString(session), IDLE_TTL.toSeconds(), TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new CacheAccessException("保存登录会话失败", exception);
        }
    }

    public Optional<LoginSession> find(String sessionId) {
        try {
            String json = redisTemplate.opsForValue().get(CacheKeys.loginUser(sessionId));
            return json == null ? Optional.empty()
                : Optional.of(objectMapper.readValue(json, LoginSession.class));
        } catch (Exception exception) {
            throw new CacheAccessException("读取登录会话失败", exception);
        }
    }

    public void touch(String sessionId) {
        redisTemplate.expire(CacheKeys.loginUser(sessionId), IDLE_TTL.toSeconds(),
            TimeUnit.SECONDS);
    }

    public void recordActivity(String sessionId, LocalDateTime time) {
        find(sessionId).ifPresent(session -> save(new LoginSession(sessionId,
            session.loginUser(), session.loginTime(), time, session.expiresAt(),
            session.revoked(), session.revokedAt())));
    }

    public void delete(String sessionId) {
        redisTemplate.delete(CacheKeys.loginUser(sessionId));
    }

    public List<LoginSession> listAll() {
        List<LoginSession> sessions = new ArrayList<>();
        try (var cursor = redisTemplate.scan(ScanOptions.scanOptions()
            .match(CacheKeys.loginUserPattern()).count(100).build())) {
            cursor.forEachRemaining(key -> find(key.substring("login:user:".length()))
                .ifPresent(sessions::add));
        }
        return sessions;
    }

    public List<String> deleteByUserId(Long userId) {
        List<String> deleted = new ArrayList<>();
        try (var cursor = redisTemplate.scan(ScanOptions.scanOptions()
            .match(CacheKeys.loginUserPattern()).count(100).build())) {
            cursor.forEachRemaining(key -> find(key.substring("login:user:".length()))
                .ifPresent(session -> {
                    if (session.loginUser().getUserId().equals(userId)) {
                        delete(session.sessionId());
                        deleted.add(session.sessionId());
                    }
                }));
        }
        return deleted;
    }
}
