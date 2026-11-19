package cn.sichu.common.security.token;

import cn.sichu.common.security.model.JwtSubject;
import cn.sichu.common.security.model.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
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
 * @since 2026/11/19
 */
@Component
public class JwtTokenService {

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtTokenService(@Value("${workspace.jwt.secret}") String secret,
        @Value("${workspace.jwt.issuer:workspace}") String issuer,
        @Value("${workspace.jwt.access-token-ttl:30m}")
        Duration accessTokenTtl) {
        if (secret == null
            || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "workspace.jwt.secret 长度必须不少于 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
    }

    public String createToken(LoginUser user, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder().issuer(issuer)
            .subject(String.valueOf(user.getUserId()))
            .id(UUID.randomUUID().toString()).issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(accessTokenTtl)))
            .claim("sid", sessionId).claim("tv", user.getTokenVersion())
            // 子应用(如 EAP)按 token_type=access + username 校验门户token，
            // 缺失会报 "workspace token 类型不匹配"
            .claim("username", user.getUsername())
            .claim("token_type", "access")
            .signWith(key, Jwts.SIG.HS256).compact();
    }

    public JwtSubject parse(String token) {
        return parse(token, false);
    }

    public JwtSubject parseIgnoringExpiration(String token) {
        return parse(token, true);
    }

    private JwtSubject parse(String token, boolean ignoreExpiration) {
        try {
            Claims claims =
                Jwts.parser().verifyWith(key).requireIssuer(issuer).build()
                    .parseSignedClaims(token).getPayload();
            if (!ignoreExpiration && claims.getExpiration().toInstant()
                .isBefore(Instant.now())) {
                throw new TokenException("token 已过期");
            }
            return new JwtSubject(Long.valueOf(claims.getSubject()),
                claims.get("sid", String.class),
                claims.get("tv", Number.class).longValue(),
                claims.getExpiration().toInstant());
        } catch (ExpiredJwtException e) {
            if (ignoreExpiration) {
                Claims claims = e.getClaims();
                return new JwtSubject(Long.valueOf(claims.getSubject()),
                    claims.get("sid", String.class),
                    claims.get("tv", Number.class).longValue(),
                    claims.getExpiration().toInstant());
            }
            throw new TokenException("token 已过期", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenException("token 无效", e);
        }
    }
}
