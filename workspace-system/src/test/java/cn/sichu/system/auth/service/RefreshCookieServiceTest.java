package cn.sichu.system.auth.service;

import cn.sichu.system.auth.model.LoginAuthentication;
import cn.sichu.system.auth.model.vo.LoginVO;
import cn.sichu.system.security.token.OpaqueAuthenticationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
class RefreshCookieServiceTest {

    @Test
    void writesConfiguredHttpOnlyCookieAndReadsIt() {
        OpaqueAuthenticationProperties properties = new OpaqueAuthenticationProperties();
        properties.getRefreshCookie().setName("custom_refresh");
        properties.getRefreshCookie().setSecure(true);
        properties.getRefreshCookie().setSameSite("Strict");
        properties.getRefreshCookie().setPath("/custom/auth");
        RefreshCookieService service = new RefreshCookieService(properties);
        MockHttpServletResponse response = new MockHttpServletResponse();
        LoginAuthentication authentication = new LoginAuthentication(
            new LoginVO("access", null), "refresh-secret", Instant.now().plusSeconds(600));

        service.write(response, authentication);

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.contains("custom_refresh=refresh-secret"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=Strict"));
        assertTrue(cookie.contains("Path=/custom/auth"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("custom_refresh", "refresh-secret"));
        assertEquals("refresh-secret", service.read(request));
    }

    @Test
    void clearsCookieWithoutRefreshValue() {
        RefreshCookieService service = new RefreshCookieService(new OpaqueAuthenticationProperties());
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.clear(response);

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.contains("workspace_refresh="));
        assertTrue(cookie.contains("Max-Age=0"));
    }
}
