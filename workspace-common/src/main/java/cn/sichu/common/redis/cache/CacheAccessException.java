package cn.sichu.common.redis.cache;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public class CacheAccessException extends RuntimeException {

    public CacheAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
