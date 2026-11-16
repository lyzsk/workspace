package cn.sichu.system.security.token;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "workspace.jwt")
public class JwtProperties {
    @NotBlank
    private String secret;

    @NotBlank
    private String issuer = "workspace";

    private Duration accessTokenTtl = Duration.ofMinutes(30);
}
