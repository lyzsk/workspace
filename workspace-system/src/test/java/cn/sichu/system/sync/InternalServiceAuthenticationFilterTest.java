package cn.sichu.system.sync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author sichu huang
 * @since 2026/11/17
 */
class InternalServiceAuthenticationFilterTest {

    private InternalServiceAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        InternalUserSyncProperties properties = new InternalUserSyncProperties();
        InternalUserSyncProperties.Client client = new InternalUserSyncProperties.Client();
        client.setAppCode("eap-tool-system");
        client.setToken("secret");
        properties.getClients().add(client);
        filter = new InternalServiceAuthenticationFilter(properties);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bindsValidCredentialToApplication() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", basic("eap-tool-system", "secret"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertEquals("eap-tool-system", authentication.getName());
            assertTrue(authentication.getAuthorities().stream()
                .anyMatch(item -> item.getAuthority()
                    .equals(InternalServiceAuthenticationFilter.AUTHORITY)));
        });
    }

    @Test
    void rejectsInvalidCredential() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", basic("eap-tool-system", "wrong"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
            assertNull(SecurityContextHolder.getContext().getAuthentication()));
    }

    @Test
    void doesNotAuthenticateOtherPaths() throws Exception {
        MockHttpServletRequest request = request();
        request.setServletPath("/api/auth/me");
        request.addHeader("Authorization", basic("eap-tool-system", "secret"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
            assertNull(SecurityContextHolder.getContext().getAuthentication()));
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/internal/users/sync");
        request.setServletPath("/api/internal/users/sync");
        return request;
    }

    private String basic(String appCode, String token) {
        return "Basic " + Base64.getEncoder().encodeToString(
            (appCode + ":" + token).getBytes(StandardCharsets.UTF_8));
    }
}
