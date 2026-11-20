package cn.sichu.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
public record ClientRequestMetadata(String ip, String userAgent) {

    public static ClientRequestMetadata current() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return new ClientRequestMetadata(null, null);
        }
        HttpServletRequest request = attributes.getRequest();
        return new ClientRequestMetadata(clientIp(request), request.getHeader("User-Agent"));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp;
    }
}
