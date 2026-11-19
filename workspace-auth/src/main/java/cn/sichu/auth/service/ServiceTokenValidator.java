package cn.sichu.auth.service;

import cn.sichu.common.security.token.TokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
public class ServiceTokenValidator {

    private final String serviceToken;

    public ServiceTokenValidator(@Value("${workspace.internal.service-token:}") String serviceToken) {
        this.serviceToken = serviceToken;
    }

    public void validate(String actual) {
        if (serviceToken == null || serviceToken.isBlank() || actual == null
            || !MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8),
                serviceToken.getBytes(StandardCharsets.UTF_8))) {
            throw new TokenException("服务凭证无效");
        }
    }
}
