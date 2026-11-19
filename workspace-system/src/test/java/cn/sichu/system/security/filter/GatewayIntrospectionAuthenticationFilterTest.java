package cn.sichu.system.security.filter;

import cn.sichu.system.security.token.OpaqueAuthenticationProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
class GatewayIntrospectionAuthenticationFilterTest {

    private GatewayIntrospectionAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        OpaqueAuthenticationProperties properties = new OpaqueAuthenticationProperties();
        properties.setGatewayToken("gateway-secret");
        filter = new GatewayIntrospectionAuthenticationFilter(properties);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void acceptsIndependentGatewayCredential() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader(GatewayIntrospectionAuthenticationFilter.TOKEN_HEADER, "gateway-secret");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertEquals("gateway", authentication.getName());
            assertTrue(authentication.getAuthorities().stream().anyMatch(
                item -> item.getAuthority().equals(GatewayIntrospectionAuthenticationFilter.AUTHORITY)));
        });
    }

    @Test
    void rejectsWrongCredentialAndOrdinaryBearerToken() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader(GatewayIntrospectionAuthenticationFilter.TOKEN_HEADER, "wrong");
        request.addHeader("Authorization", "Bearer ordinary-user-token");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
            assertNull(SecurityContextHolder.getContext().getAuthentication()));
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/internal/auth/introspect");
        request.setServletPath("/api/internal/auth/introspect");
        return request;
    }
}
