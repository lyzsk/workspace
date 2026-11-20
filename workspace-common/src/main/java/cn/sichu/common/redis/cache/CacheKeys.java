package cn.sichu.common.redis.cache;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    public static String loginUser(String sessionId) {
        return "login:user:" + sessionId;
    }

    public static String loginUserPattern() {
        return "login:user:*";
    }

    public static String authorization(Long userId, String appCode) {
        return "authz:" + userId + ":" + appCode;
    }

    public static String authorizationPattern(String appCode) {
        return "authz:*:" + appCode;
    }

    public static String authorizationUserPattern(Long userId) {
        return "authz:" + userId + ":*";
    }

    public static String apiRequestHour(String hour) {
        return "metrics:api:hour:" + hour;
    }
}
