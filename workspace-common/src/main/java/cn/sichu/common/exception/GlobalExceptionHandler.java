package cn.sichu.common.exception;

import cn.sichu.common.result.IResultCode;
import cn.sichu.common.result.Result;
import cn.sichu.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        IResultCode code = e.getResultCode();
        log.warn("业务异常 code={} msg={}", code.getCode(), e.getMessage());
        return ResponseEntity.status(code.getCode()).body(Result.failed(code, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.status(ResultCode.PARAM_ERROR.getCode())
            .body(Result.failed(ResultCode.PARAM_ERROR, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(ResultCode.PARAM_ERROR.getCode())
            .body(Result.failed(ResultCode.PARAM_ERROR, "请求体格式错误"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getResourcePath());
        return ResponseEntity.status(ResultCode.NOT_FOUND.getCode())
            .body(Result.failed(ResultCode.NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(ResultCode.FAILED.getCode()).body(Result.failed());
    }
}
