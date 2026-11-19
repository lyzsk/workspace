package cn.sichu.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdWebFilter implements WebFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final Pattern VALID_REQUEST_ID =
        Pattern.compile("^[A-Za-z0-9._-]{1,128}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId =
            exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || !VALID_REQUEST_ID.matcher(requestId)
            .matches()) {
            requestId = UUID.randomUUID().toString();
        }
        String effectiveRequestId = requestId;
        exchange.getResponse().getHeaders()
            .set(REQUEST_ID_HEADER, effectiveRequestId);
        ServerWebExchange mutatedExchange = exchange.mutate().request(
                builder -> builder.headers(
                    headers -> headers.set(REQUEST_ID_HEADER, effectiveRequestId)))
            .build();
        return chain.filter(mutatedExchange);
    }
}
