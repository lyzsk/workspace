package cn.sichu.gateway.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthenticationWebFilterTest {

    private GatewayAuthenticationProperties properties;
    private GatewayErrorResponseWriter errorWriter;

    @BeforeEach
    void setUp() {
        properties = new GatewayAuthenticationProperties();
        properties.setGatewayToken("internal-secret");
        properties.setIntrospectionTimeout(Duration.ofSeconds(1));
        errorWriter = new GatewayErrorResponseWriter(new ObjectMapper());
    }

    @Test
    void publicRequestSkipsIntrospectionAndRemovesSpoofedIdentity() {
        AtomicReference<ServerWebExchangeCapture> captured = new AtomicReference<>();
        AuthenticationWebFilter filter = filter(WebClient.builder());
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/auth/login")
                .header(AuthenticationWebFilter.USER_ID_HEADER, "999")
                .header(AuthenticationWebFilter.GATEWAY_TOKEN_HEADER, "fake"));

        filter.filter(exchange, current -> {
            captured.set(new ServerWebExchangeCapture(
                current.getRequest().getHeaders().getFirst(AuthenticationWebFilter.USER_ID_HEADER),
                current.getRequest().getHeaders().getFirst(AuthenticationWebFilter.GATEWAY_TOKEN_HEADER)));
            return Mono.empty();
        }).block();

        assertNull(captured.get().userId());
        assertNull(captured.get().gatewayToken());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void missingTokenReturns401() {
        AuthenticationWebFilter filter = filter(WebClient.builder());
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/system/users"));

        filter.filter(exchange, current -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void internalEndpointCannotBeCalledThroughGateway() {
        AuthenticationWebFilter filter = filter(WebClient.builder());
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post(INTROSPECTION_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer x"));

        filter.filter(exchange, current -> Mono.empty()).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void activeTokenAddsTrustedIdentityAndRemovesBearer() {
        WebClient.Builder builder = webClientWithResponse(HttpStatus.OK,
            "{\"active\":true,\"userId\":1,\"username\":\"admin\",\"tenantId\":0,\"sessionId\":\"s1\",\"tokenVersion\":0}");
        AuthenticationWebFilter filter = filter(builder);
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/system/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer opaque"));
        AtomicReference<org.springframework.http.HttpHeaders> headers = new AtomicReference<>();

        filter.filter(exchange, current -> {
            headers.set(current.getRequest().getHeaders());
            return Mono.empty();
        }).block();

        assertEquals("1", headers.get().getFirst(AuthenticationWebFilter.USER_ID_HEADER));
        assertEquals("s1", headers.get().getFirst(AuthenticationWebFilter.SESSION_ID_HEADER));
        assertEquals("internal-secret", headers.get().getFirst(AuthenticationWebFilter.GATEWAY_TOKEN_HEADER));
        assertNull(headers.get().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void inactiveTokenReturns401() {
        AuthenticationWebFilter filter = filter(webClientWithResponse(HttpStatus.OK,
            "{\"active\":false}"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/system/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer opaque"));

        filter.filter(exchange, current -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void introspectionFailureReturns503() {
        AuthenticationWebFilter filter = filter(webClientWithResponse(HttpStatus.SERVICE_UNAVAILABLE, ""));
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/system/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer opaque"));

        filter.filter(exchange, current -> Mono.empty()).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }

    @Test
    void emptyIntrospectionResponseReturns503() {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request ->
            Mono.just(ClientResponse.create(HttpStatus.OK).build()));
        AuthenticationWebFilter filter = filter(builder);
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/system/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer opaque"));

        filter.filter(exchange, current -> Mono.empty()).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }

    private AuthenticationWebFilter filter(WebClient.Builder builder) {
        return new AuthenticationWebFilter(builder, properties, errorWriter);
    }

    private WebClient.Builder webClientWithResponse(HttpStatus status, String body) {
        return WebClient.builder().exchangeFunction(request -> Mono.just(ClientResponse.create(status)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).body(body).build()));
    }

    private static final String INTROSPECTION_PATH = "/api/internal/auth/introspect";

    private record ServerWebExchangeCapture(String userId, String gatewayToken) {
    }
}
