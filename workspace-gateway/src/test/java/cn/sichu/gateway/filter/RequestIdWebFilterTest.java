package cn.sichu.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestIdWebFilterTest {

    private final RequestIdWebFilter filter = new RequestIdWebFilter();

    @Test
    void preservesValidRequestId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/auth/me").header(RequestIdWebFilter.REQUEST_ID_HEADER, "request-001"));
        AtomicReference<String> downstreamRequestId = new AtomicReference<>();
        WebFilterChain chain = current -> {
            downstreamRequestId.set(current.getRequest().getHeaders().getFirst(RequestIdWebFilter.REQUEST_ID_HEADER));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertEquals("request-001", downstreamRequestId.get());
        assertEquals("request-001", exchange.getResponse().getHeaders().getFirst(RequestIdWebFilter.REQUEST_ID_HEADER));
    }

    @Test
    void replacesInvalidRequestId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/ws/notifications").header(RequestIdWebFilter.REQUEST_ID_HEADER, "invalid id"));
        AtomicReference<String> downstreamRequestId = new AtomicReference<>();
        WebFilterChain chain = current -> {
            downstreamRequestId.set(current.getRequest().getHeaders().getFirst(RequestIdWebFilter.REQUEST_ID_HEADER));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        String requestId = downstreamRequestId.get();
        assertNotNull(requestId);
        assertNotEquals("invalid id", requestId);
        assertTrue(requestId.matches("^[A-Za-z0-9._-]{1,128}$"));
        assertEquals(requestId, exchange.getResponse().getHeaders().getFirst(RequestIdWebFilter.REQUEST_ID_HEADER));
    }
}
