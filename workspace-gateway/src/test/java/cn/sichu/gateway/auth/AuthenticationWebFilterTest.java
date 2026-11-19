package cn.sichu.gateway.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
class AuthenticationWebFilterTest {

    @Test
    void allowsKnife4jDocumentWithoutAuthorization() {
        assertPublicDocument("/doc.html");
    }

    @Test
    void allowsOpenApiDocumentWithoutAuthorization() {
        assertPublicDocument("/v3/api-docs/default");
    }

    private void assertPublicDocument(String path) {
        GatewayTokenVerifier verifier = mock(GatewayTokenVerifier.class);
        AuthenticationWebFilter filter = new AuthenticationWebFilter(verifier,
            new GatewayAuthenticationProperties(), mock(GatewayErrorResponseWriter.class));
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get(path));

        filter.filter(exchange, ignored -> Mono.empty()).block();

        verifyNoInteractions(verifier);
    }
}
