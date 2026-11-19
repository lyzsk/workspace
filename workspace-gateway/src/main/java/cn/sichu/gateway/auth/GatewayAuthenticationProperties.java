package cn.sichu.gateway.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
@ConfigurationProperties(prefix = "workspace.auth")
public class GatewayAuthenticationProperties {

    private String gatewayToken;

    private Duration introspectionTimeout = Duration.ofSeconds(3);

    public String getGatewayToken() {
        return gatewayToken;
    }

    public void setGatewayToken(String gatewayToken) {
        this.gatewayToken = gatewayToken;
    }

    public Duration getIntrospectionTimeout() {
        return introspectionTimeout;
    }

    public void setIntrospectionTimeout(Duration introspectionTimeout) {
        this.introspectionTimeout = introspectionTimeout;
    }
}
