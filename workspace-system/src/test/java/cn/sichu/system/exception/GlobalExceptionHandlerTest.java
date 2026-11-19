package cn.sichu.system.exception;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.core.exception.GlobalExceptionHandler;
import cn.sichu.common.core.result.Result;
import cn.sichu.system.code.SystemCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 *
 * @author sichu huang
 * @since 2026/11/17
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessExceptionUsesMatchingHttpAndBodyCode() {
        ResponseEntity<Result<Void>> response =
            handler.handleBusinessException(new BusinessException(SystemCode.USER_NOT_FOUND));

        assertEquals(404, response.getStatusCode().value());
        assertEquals(404, response.getBody().getCode());
    }

    @Test
    void systemExceptionReturns500WithoutInternalMessage() {
        ResponseEntity<Result<Void>> response =
            handler.handleException(new IllegalStateException("sensitive detail"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals(500, response.getBody().getCode());
        assertFalse(response.getBody().getMsg().contains("sensitive detail"));
    }
}
