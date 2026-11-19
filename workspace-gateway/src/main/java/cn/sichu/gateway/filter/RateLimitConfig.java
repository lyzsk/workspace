package cn.sichu.gateway.filter;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Configuration
public class RateLimitConfig {

    @Bean
    KeyResolver clientIpKeyResolver() {
        return exchange -> Mono.just(clientIp(exchange));
    }

    @Bean
    @org.springframework.context.annotation.Primary
    KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders()
                .getFirst("X-Workspace-User-Id");
            return Mono.just(
                userId == null ? "ip:" + clientIp(exchange) : "user:" + userId);
        };
    }

    private String clientIp(ServerWebExchange exchange) {
        return exchange.getRequest().getRemoteAddress() == null
            || exchange.getRequest().getRemoteAddress().getAddress() == null ?
            "unknown" : exchange.getRequest().getRemoteAddress().getAddress()
            .getHostAddress();
    }
}
