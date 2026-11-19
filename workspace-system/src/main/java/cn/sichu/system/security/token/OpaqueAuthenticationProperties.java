package cn.sichu.system.security.token;

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
@ConfigurationProperties(prefix = "workspace.auth")
public class OpaqueAuthenticationProperties {

    private final RefreshCookie refreshCookie = new RefreshCookie();
    private Duration accessTokenTtl = Duration.ofMinutes(30);
    private String gatewayToken = "";

    @Getter
    @Setter
    public static class RefreshCookie {

        private String name = "workspace_refresh";

        private boolean secure;

        private String sameSite = "Lax";

        private String path = "/api/auth";
    }
}
