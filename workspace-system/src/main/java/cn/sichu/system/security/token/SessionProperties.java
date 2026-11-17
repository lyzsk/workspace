package cn.sichu.system.security.token;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 *
 * @author sichu huang
 * @since 2026/11/17
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "workspace.session")
public class SessionProperties {

    private Duration idleTimeout = Duration.ofMinutes(30);
}
