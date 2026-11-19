package cn.sichu.gateway.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Duration;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
public class GatewayTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GatewayTokenVerifier.class);

    private final SecretKey key;
    private final String issuer;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public GatewayTokenVerifier(
        @Value("${workspace.jwt.secret}") String secret,
        @Value("${workspace.jwt.issuer:workspace}") String issuer,
        ReactiveStringRedisTemplate redisTemplate,
        ObjectMapper objectMapper) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Mono<VerifiedIdentity> verify(String token) {
        return Mono.defer(() -> {
            try {
                Claims claims = Jwts.parser().verifyWith(key).requireIssuer(issuer).build()
                    .parseSignedClaims(token).getPayload();
                if (claims.getExpiration().toInstant().isBefore(Instant.now())) {
                    return Mono.empty();
                }
                Long userId = Long.valueOf(claims.getSubject());
                String sessionId = claims.get("sid", String.class);
                Long tokenVersion = claims.get("tv", Number.class).longValue();
                return redisTemplate.opsForValue().get("login:user:" + sessionId)
                    .flatMap(json -> approve(json, userId, sessionId, tokenVersion))
                    .switchIfEmpty(Mono.fromRunnable(() ->
                        log.warn("登录会话不存在或无效 sessionId={}", sessionId)).then(Mono.empty()));
            } catch (Exception exception) {
                log.warn("token 解析失败: {}", exception.getMessage());
                return Mono.empty();
            }
        });
    }

    private Mono<VerifiedIdentity> approve(String json, Long userId, String sessionId,
        Long tokenVersion) {
        try {
            JsonNode session = objectMapper.readTree(json);
            JsonNode user = session.path("loginUser");
            LocalDateTime expiresAt = LocalDateTime.parse(session.path("expiresAt").asText());
            if (session.path("revoked").asBoolean(false)
                || expiresAt.isBefore(LocalDateTime.now())
                || user.path("userId").asLong() != userId
                || user.path("tokenVersion").asLong(-1) != tokenVersion
                || user.path("status").asInt(0) != 1) {
                return Mono.empty();
            }
            return redisTemplate.expire("login:user:" + sessionId, Duration.ofMinutes(30))
                .thenReturn(new VerifiedIdentity(userId, sessionId, tokenVersion,
                    user.path("username").asText(null), user.path("tenantId").asText(null)));
        } catch (Exception exception) {
            return Mono.empty();
        }
    }

    public record VerifiedIdentity(Long userId, String sessionId, Long tokenVersion,
                                   String username, String tenantId) {
    }
}
