package cn.sichu.system.monitor.model.vo;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public record ServerInfoVO(String hostName, String osName, String osVersion, String javaVersion,
                           String javaVendor, Integer cpuCores, Long totalMemoryMb,
                           Long usedMemoryMb, Long freeMemoryMb, Long maxMemoryMb,
                           Long nonHeapUsedMb, Long uptimeSeconds, String serverTime) {
}
