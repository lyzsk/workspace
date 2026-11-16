package cn.sichu.system.app.service.impl;

import cn.sichu.system.app.config.EapToolServiceTicketProperties;
import cn.sichu.system.app.model.vo.EapToolServiceTicketVO;
import cn.sichu.system.app.service.IEapToolServiceTicketService;
import cn.sichu.system.security.model.LoginUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@Slf4j
@Service
public class EapToolServiceTicketServiceImpl implements IEapToolServiceTicketService {

    private static final int MIN_SECRET_BYTES = 32;

    private final EapToolServiceTicketProperties properties;
    private final SecretKey key;

    public EapToolServiceTicketServiceImpl(EapToolServiceTicketProperties properties) {
        this.properties = properties;
        String secret = properties.getTicketSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("workspace.eap-tool-service.ticket-secret 未配置");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "workspace.eap-tool-service.ticket-secret 长度必须不少于 " + MIN_SECRET_BYTES + " 字节");
        }
        if (secret.startsWith("dev-only-")) {
            log.warn("正在使用开发默认 EAP ticket 密钥，生产环境必须通过环境变量 "
                + "EAP_TOOL_SERVICE_TICKET_SECRET 覆盖");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    @Override
    public EapToolServiceTicketVO issue(LoginUser user) {
        Instant now = Instant.now();
        Duration ttl = properties.getTicketTtl();
        String ticket = Jwts.builder()
            .issuer(properties.getIssuer())
            .audience().add(properties.getAudience()).and()
            .subject(String.valueOf(user.getUserId()))
            .claim("username", user.getUsername())
            .claim("realName", user.getRealName())
            .claim("tenantId", user.getTenantId())
            .claim("roles", user.getRoles())
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(ttl)))
            .signWith(key, Jwts.SIG.HS256)
            .compact();
        return EapToolServiceTicketVO.of(ticket, normalizeUrl(properties.getUiUrl()),
            ttl.toSeconds());
    }

    private String normalizeUrl(String url) {
        return url.endsWith("/") ? url : url + "/";
    }
}
