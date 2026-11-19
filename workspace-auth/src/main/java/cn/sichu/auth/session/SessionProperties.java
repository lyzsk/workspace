package cn.sichu.auth.session;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "workspace.session")
public class SessionProperties {

    private Duration idleTimeout = Duration.ofMinutes(30);

    private Duration absoluteTimeout = Duration.ofHours(12);
}
