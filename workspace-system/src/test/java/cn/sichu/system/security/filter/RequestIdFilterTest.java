package cn.sichu.system.security.filter;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author sichu huang
 * @since 2026/11/17
 */
class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void acceptsValidRequestIdAndClearsMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "request-001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response,
            (req, res) -> assertEquals("request-001", MDC.get(RequestIdFilter.MDC_KEY)));

        assertEquals("request-001", response.getHeader(RequestIdFilter.REQUEST_ID_HEADER));
        assertNull(MDC.get(RequestIdFilter.MDC_KEY));
    }

    @Test
    void replacesInvalidRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "invalid id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        String requestId = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        assertNotEquals("invalid id", requestId);
        assertTrue(requestId.matches("^[A-Za-z0-9._-]{1,128}$"));
        assertNull(MDC.get(RequestIdFilter.MDC_KEY));
    }
}
