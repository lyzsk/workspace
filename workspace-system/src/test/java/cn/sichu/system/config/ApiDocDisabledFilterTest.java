package cn.sichu.system.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
class ApiDocDisabledFilterTest {

    private final ApiDocDisabledFilter filter = new ApiDocDisabledFilter();

    @Test
    void returns404ForDocumentation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/doc.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(404, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void passesBusinessRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(request, response);
    }
}
