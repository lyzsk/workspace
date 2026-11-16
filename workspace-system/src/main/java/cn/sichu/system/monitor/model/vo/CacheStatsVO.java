package cn.sichu.system.monitor.model.vo;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public record CacheStatsVO(Long size, Long hits, Long misses, Double hitRate) {
}
