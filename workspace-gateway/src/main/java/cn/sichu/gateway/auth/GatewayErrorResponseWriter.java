package cn.sichu.gateway.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
public class GatewayErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public GatewayErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status,
        String message) {
        try {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", status.value());
            error.put("msg", message);
            error.put("data", null);
            byte[] body = objectMapper.writeValueAsBytes(error);
            exchange.getResponse().setStatusCode(status);
            exchange.getResponse().getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);
            return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
        } catch (Exception exception) {
            return Mono.error(exception);
        }
    }
}
