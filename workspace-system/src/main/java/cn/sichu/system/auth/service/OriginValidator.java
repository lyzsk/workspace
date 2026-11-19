package cn.sichu.system.auth.service;

import cn.sichu.common.exception.BusinessException;
import cn.sichu.common.result.ResultCode;
import cn.sichu.system.config.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Component
@RequiredArgsConstructor
public class OriginValidator {

    private final CorsProperties corsProperties;

    public void validate(String origin) {
        if (origin == null || !corsProperties.getAllowedOrigins().contains(origin)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "请求来源不受信任");
        }
    }
}
