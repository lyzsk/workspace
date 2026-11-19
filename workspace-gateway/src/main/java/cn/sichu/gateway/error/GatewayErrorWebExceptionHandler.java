package cn.sichu.gateway.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
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
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayErrorWebExceptionHandler
    implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GatewayErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        HttpStatusCode status = ex instanceof ErrorResponse errorResponse ?
            errorResponse.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", status.value());
        body.put("msg", message(status));
        body.put("data", null);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException serializationException) {
            return Mono.error(serializationException);
        }
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders()
            .setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(
            Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private String message(HttpStatusCode status) {
        if (status.value() == 400) {
            return "请求参数错误";
        }
        if (status.value() == 401) {
            return "未登录或登录已过期";
        }
        if (status.value() == 403) {
            return "没有权限";
        }
        if (status.value() == 404) {
            return "资源不存在";
        }
        if (status.value() == 409) {
            return "资源状态冲突";
        }
        if (status.value() == 503) {
            return "服务暂不可用";
        }
        return "操作失败";
    }
}
