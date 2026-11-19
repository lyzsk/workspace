package cn.sichu.gateway.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GatewayErrorResponseWriter {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorResponseWriter.class);

    private final ObjectMapper objectMapper;

    public GatewayErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status,
        String message) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }
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
        } catch (UnsupportedOperationException exception) {
            log.warn("响应头已只读，跳过错误体写入 status={} msg={}", status.value(),
                exception.getMessage());
            return Mono.empty();
        } catch (Exception exception) {
            return Mono.error(exception);
        }
    }
}
