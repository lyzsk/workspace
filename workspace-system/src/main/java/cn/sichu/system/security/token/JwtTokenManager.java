package cn.sichu.system.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Slf4j
@Component
public class JwtTokenManager {

    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_TENANT_ID = "tenant_id";
    private static final String CLAIM_TOKEN_VERSION = "token_version";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenManager(JwtProperties properties) {
        this.properties = properties;
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("workspace.jwt.secret 未配置");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "workspace.jwt.secret 长度必须不少于 " + MIN_SECRET_BYTES
                    + " 字节（HS256）");
        }
        if (secret.startsWith("dev-only-")) {
            log.warn(
                "正在使用开发默认 JWT 密钥，生产环境必须通过环境变量 JWT_SECRET 覆盖");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String createAccessToken(Long userId, String username, Long tenantId,
        Long tokenVersion) {
        return create(userId, username, tenantId, tokenVersion,
            properties.getAccessTokenTtl());
    }

    public TokenPayload parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key)
                .requireIssuer(properties.getIssuer()).build()
                .parseSignedClaims(token).getPayload();

            String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!TOKEN_TYPE_ACCESS.equals(tokenType)) {
                throw new TokenException("token 类型不匹配");
            }

            Number tenantIdClaim = claims.get(CLAIM_TENANT_ID, Number.class);
            Number tokenVersionClaim =
                claims.get(CLAIM_TOKEN_VERSION, Number.class);
            return new TokenPayload(Long.valueOf(claims.getSubject()),
                claims.get(CLAIM_USERNAME, String.class),
                tenantIdClaim == null ? null : tenantIdClaim.longValue(),
                tokenVersionClaim == null ? 0L : tokenVersionClaim.longValue(),
                claims.getId(), claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant());
        } catch (ExpiredJwtException e) {
            throw new TokenException("token 已过期", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenException("token 无效", e);
        }
    }

    private String create(Long userId, String username, Long tenantId,
        Long tokenVersion, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder().issuer(properties.getIssuer())
            .subject(String.valueOf(userId)).id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now)).expiration(Date.from(now.plus(ttl)))
            .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
            .claim(CLAIM_USERNAME, username).claim(CLAIM_TENANT_ID, tenantId)
            .claim(CLAIM_TOKEN_VERSION, tokenVersion)
            .signWith(key, Jwts.SIG.HS256).compact();
    }
}
