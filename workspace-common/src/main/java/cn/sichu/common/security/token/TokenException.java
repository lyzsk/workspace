package cn.sichu.common.security.token;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public class TokenException extends RuntimeException {
    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
