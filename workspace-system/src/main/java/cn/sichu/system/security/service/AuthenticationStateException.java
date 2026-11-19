package cn.sichu.system.security.service;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public class AuthenticationStateException extends RuntimeException {

    public AuthenticationStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthenticationStateException(String message) {
        super(message);
    }
}
