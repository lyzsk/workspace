package cn.sichu.system.log.aspect;

import cn.sichu.common.utils.TextUtils;
import cn.sichu.common.log.annotation.Log;
import cn.sichu.system.log.entity.SysOperLog;
import cn.sichu.system.log.service.ILogService;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.util.SecurityUtils;
import cn.sichu.system.security.util.WebRequestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private static final int MAX_TEXT = 4000;
    private static final int MAX_ERROR = 2000;

    private final ILogService logService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, Log logAnnotation) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            error = throwable;
            throw throwable;
        } finally {
            try {
                save(joinPoint, logAnnotation, result, error,
                    System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.warn("记录操作日志失败: {}", e.getMessage());
            }
        }
    }

    private void save(ProceedingJoinPoint joinPoint, Log logAnnotation, Object result,
        Throwable error, long costTime) {
        SysOperLog entry = new SysOperLog();
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        if (loginUser != null) {
            entry.setUserId(loginUser.getUserId());
            entry.setUsername(loginUser.getUsername());
            entry.setTenantId(loginUser.getTenantId());
        } else {
            entry.setTenantId(0L);
        }
        entry.setModule(logAnnotation.module());
        entry.setOperation(logAnnotation.operation());
        entry.setRequestMethod(WebRequestUtils.currentRequest() == null ? null
            : WebRequestUtils.currentRequest().getMethod());
        HttpServletRequest request = WebRequestUtils.currentRequest();
        entry.setRequestUri(request == null ? null : request.getRequestURI());
        entry.setRequestParams(toJson(joinPoint.getArgs()));
        entry.setResponseResult(toJson(result));
        entry.setStatus(error == null ? 1 : 0);
        entry.setErrorMsg(error == null ? null : TextUtils.truncate(error.getMessage(), MAX_ERROR));
        entry.setCostTime(costTime);
        entry.setIp(WebRequestUtils.clientIp());
        entry.setOperTime(LocalDateTime.now());
        logService.record(entry);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        Object target = value;
        if (value instanceof Object[] args) {
            target = Arrays.stream(args)
                .filter(arg -> !(arg instanceof ServletRequest)
                    && !(arg instanceof ServletResponse)
                    && !(arg instanceof MultipartFile)
                    && !(arg instanceof BindingResult))
                .toArray();
        }
        try {
            return TextUtils.truncate(maskPassword(objectMapper.writeValueAsString(target)), MAX_TEXT);
        } catch (Exception e) {
            return TextUtils.truncate(maskPassword(String.valueOf(target)), MAX_TEXT);
        }
    }

    private String maskPassword(String text) {
        return text.replaceAll("(?i)(\"password\"\\s*:\\s*\")[^\"]*(\")", "$1******$2");
    }
}
