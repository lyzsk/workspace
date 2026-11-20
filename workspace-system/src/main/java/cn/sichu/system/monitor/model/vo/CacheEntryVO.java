package cn.sichu.system.monitor.model.vo;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public record CacheEntryVO(String key, String value, Long ttlSeconds) {
}
