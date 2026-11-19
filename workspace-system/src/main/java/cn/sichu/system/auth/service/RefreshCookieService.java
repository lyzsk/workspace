package cn.sichu.system.auth.service;

import cn.sichu.system.auth.model.LoginAuthentication;
import cn.sichu.system.security.token.OpaqueAuthenticationProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
@RequiredArgsConstructor
public class RefreshCookieService {

    private final OpaqueAuthenticationProperties properties;

    public String read(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (properties.getRefreshCookie().getName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public void write(HttpServletResponse response, LoginAuthentication authentication) {
        Duration maxAge = Duration.between(Instant.now(), authentication.sessionExpiresAt());
        ResponseCookie cookie = builder(authentication.refreshToken())
            .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, builder("").maxAge(Duration.ZERO).build().toString());
    }

    private ResponseCookie.ResponseCookieBuilder builder(String value) {
        OpaqueAuthenticationProperties.RefreshCookie properties = this.properties.getRefreshCookie();
        return ResponseCookie.from(properties.getName(), value).httpOnly(true)
            .secure(properties.isSecure()).sameSite(properties.getSameSite())
            .path(properties.getPath());
    }
}
