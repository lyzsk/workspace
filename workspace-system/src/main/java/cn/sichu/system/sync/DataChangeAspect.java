package cn.sichu.system.sync;

import cn.sichu.common.core.event.DataChangedEvent;
import cn.sichu.common.core.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DataChangeAspect {

    private static final String[] EXCLUDED_PREFIXES = {
        "/api/auth", "/api/files", "/api/eap-tool-service", "/api/system/documents",
        "/api/system/messages", "/api/system/notification-preferences",
        "/api/system/dashboard/access-events"
    };

    private final ApplicationEventPublisher eventPublisher;

    @AfterReturning(pointcut = "execution(public * cn.sichu.system..controller..*(..))",
        returning = "result")
    public void afterReturning(Object result) {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            String method = request.getMethod();
            if ("GET".equals(method) || "OPTIONS".equals(method) || "HEAD".equals(method)) {
                return;
            }
            String uri = request.getRequestURI();
            if (excluded(uri)) {
                return;
            }
            if (!(result instanceof Result<?> response) || !Result.isSuccess(response)) {
                return;
            }
            eventPublisher.publishEvent(new DataChangedEvent(uri, method, LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("数据变更事件发布失败: {}", e.getMessage());
        }
    }

    private boolean excluded(String uri) {
        if (uri == null || uri.contains("/attachments")) {
            return true;
        }
        for (String prefix : EXCLUDED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
