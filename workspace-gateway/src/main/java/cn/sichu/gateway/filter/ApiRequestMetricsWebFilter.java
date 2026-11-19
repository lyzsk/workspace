package cn.sichu.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApiRequestMetricsWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiRequestMetricsWebFilter.class);
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final Duration RETENTION = Duration.ofDays(35);

    private final ReactiveStringRedisTemplate redisTemplate;

    public ApiRequestMetricsWebFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!exchange.getRequest().getPath().value().startsWith("/api/")) {
            return chain.filter(exchange);
        }
        String key = "metrics:api:hour:" + LocalDateTime.now().format(HOUR_FORMAT);
        return redisTemplate.opsForValue().increment(key)
            .flatMap(count -> count == 1
                ? redisTemplate.expire(key, RETENTION).thenReturn(count)
                : Mono.just(count))
            .doOnError(error -> log.warn("REST 请求计数写入失败: {}", error.getMessage()))
            .onErrorResume(error -> Mono.empty())
            .then(chain.filter(exchange));
    }
}
