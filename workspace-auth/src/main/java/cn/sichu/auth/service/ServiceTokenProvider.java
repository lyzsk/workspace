package cn.sichu.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
public class ServiceTokenProvider {

    private final String serviceToken;

    public ServiceTokenProvider(@Value("${workspace.internal.service-token:}") String serviceToken) {
        this.serviceToken = serviceToken;
    }

    public String serviceToken() {
        return serviceToken;
    }
}
