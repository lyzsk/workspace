package cn.sichu.gateway.error;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayErrorWebExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GatewayErrorWebExceptionHandler handler = new GatewayErrorWebExceptionHandler(objectMapper);

    @Test
    void writesResultShapedNotFoundResponse() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/missing"));

        handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND)).block();

        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertEquals(404, body.get("code").asInt());
        assertEquals("资源不存在", body.get("msg").asText());
        assertTrue(body.get("data").isNull());
    }

    @Test
    void usesSameNumericCodeForServiceUnavailable() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/me"));

        handler.handle(exchange, new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertEquals(503, body.get("code").asInt());
        assertEquals("服务暂不可用", body.get("msg").asText());
    }
}
