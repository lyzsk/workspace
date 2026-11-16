package cn.sichu.system.security.handler;

import cn.sichu.common.result.Result;
import cn.sichu.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.failed(ResultCode.FORBIDDEN);
    }
}
