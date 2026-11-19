package cn.sichu.gateway.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
class GatewayTokenVerifierTest {

    private final String secret = "0123456789abcdef0123456789abcdef";

    @Test
    void acceptsActiveSessionAndRefreshesIdleTtl() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        String json = "{\"loginUser\":{\"userId\":1,\"username\":\"admin\","
            + "\"tokenVersion\":0,\"status\":1,\"tenantId\":0},"
            + "\"expiresAt\":\"" + LocalDateTime.now().plusHours(12) + "\",\"revoked\":false}";
        when(values.get("login:user:s1")).thenReturn(Mono.just(json));
        when(redis.expire(anyString(), any(java.time.Duration.class))).thenReturn(Mono.just(true));
        GatewayTokenVerifier verifier = new GatewayTokenVerifier(secret, "workspace", redis,
            new ObjectMapper());

        GatewayTokenVerifier.VerifiedIdentity identity = verifier.verify(token("s1", 0L)).block();

        assertNotNull(identity);
        assertEquals(1L, identity.userId());
        verify(redis).expire(eq("login:user:s1"), any(java.time.Duration.class));
    }

    @Test
    void rejectsExpiredSession() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        String json = "{\"loginUser\":{\"userId\":1,\"tokenVersion\":0,\"status\":1},"
            + "\"expiresAt\":\"" + LocalDateTime.now().minusMinutes(1) + "\",\"revoked\":false}";
        when(values.get("login:user:s1")).thenReturn(Mono.just(json));
        GatewayTokenVerifier verifier = new GatewayTokenVerifier(secret, "workspace", redis,
            new ObjectMapper());

        assertNull(verifier.verify(token("s1", 0L)).block());
    }

    private String token(String sessionId, Long tokenVersion) {
        return Jwts.builder().issuer("workspace").subject("1")
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .claim("sid", sessionId).claim("tv", tokenVersion)
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)),
                Jwts.SIG.HS256).compact();
    }
}
