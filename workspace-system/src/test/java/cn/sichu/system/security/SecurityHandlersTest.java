package cn.sichu.system.security;

import cn.sichu.common.result.Result;
import cn.sichu.system.security.handler.ApiErrorWriter;
import cn.sichu.system.security.handler.RestAccessDeniedHandler;
import cn.sichu.system.security.handler.RestAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
class SecurityHandlersTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void entryPointReturns401WithUnauthorizedCode() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint(new ApiErrorWriter(objectMapper)).commence(null, response,
            new BadCredentialsException("not logged in"));

        assertEquals(401, response.getStatus());
        Result<?> body = objectMapper.readValue(response.getContentAsString(), Result.class);
        assertEquals("A0004", body.getCode());
    }

    @Test
    void accessDeniedReturns403WithForbiddenCode() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAccessDeniedHandler(new ApiErrorWriter(objectMapper)).handle(null, response,
            new AccessDeniedException("denied"));

        assertEquals(403, response.getStatus());
        Result<?> body = objectMapper.readValue(response.getContentAsString(), Result.class);
        assertEquals("A0005", body.getCode());
    }
}
