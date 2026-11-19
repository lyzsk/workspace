package cn.sichu.common.redis.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
public class AuthorizationStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AuthorizationStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(Long userId, String appCode, Object authorization) {
        try {
            redisTemplate.opsForValue().set(CacheKeys.authorization(userId, appCode),
                objectMapper.writeValueAsString(authorization), 30, TimeUnit.MINUTES);
        } catch (Exception exception) {
            throw new CacheAccessException("保存应用授权缓存失败", exception);
        }
    }

    public Optional<JsonNode> find(Long userId, String appCode) {
        try {
            String json = redisTemplate.opsForValue().get(CacheKeys.authorization(userId, appCode));
            return json == null ? Optional.empty() : Optional.of(objectMapper.readTree(json));
        } catch (Exception exception) {
            throw new CacheAccessException("读取应用授权缓存失败", exception);
        }
    }

    public void delete(Long userId, String appCode) {
        redisTemplate.delete(CacheKeys.authorization(userId, appCode));
    }

    public void deleteApplication(String appCode) {
        try (var cursor = redisTemplate.scan(
            ScanOptions.scanOptions().match(CacheKeys.authorizationPattern(appCode)).count(100)
                .build())) {
            cursor.forEachRemaining(redisTemplate::delete);
        }
    }
}
