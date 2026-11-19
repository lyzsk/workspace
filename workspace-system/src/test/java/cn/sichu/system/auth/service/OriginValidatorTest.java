package cn.sichu.system.auth.service;

import cn.sichu.common.exception.BusinessException;
import cn.sichu.system.config.CorsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
class OriginValidatorTest {

    private OriginValidator validator;

    @BeforeEach
    void setUp() {
        CorsProperties properties = new CorsProperties();
        properties.getAllowedOrigins().clear();
        properties.getAllowedOrigins().add("https://workspace.example.com");
        validator = new OriginValidator(properties);
    }

    @Test
    void acceptsConfiguredOrigin() {
        assertDoesNotThrow(() -> validator.validate("https://workspace.example.com"));
    }

    @Test
    void rejectsMissingAndUntrustedOrigins() {
        assertThrows(BusinessException.class, () -> validator.validate(null));
        assertThrows(BusinessException.class, () -> validator.validate("https://attacker.example.com"));
    }
}
