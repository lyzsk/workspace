import { request } from "@/utils/http";

export interface ServerInfo {
    hostName: string;
    osName: string;
    osVersion: string;
    javaVersion: string;
    javaVendor: string;
    cpuCores: number;
    totalMemoryMb: number;
    usedMemoryMb: number;
    freeMemoryMb: number;
    maxMemoryMb: number;
    nonHeapUsedMb: number;
    uptimeSeconds: number;
    serverTime: string;
}

export interface CacheStats {
    size: number;
    hits: number;
    misses: number;
    hitRate: number;
}

export interface CacheEntry {
    key: string;
    value: string;
    updateTime: string;
}

export function getServerInfo() {
    return request<ServerInfo>({ url: "/monitor/server", method: "get" });
}

export function getCacheStats() {
    return request<CacheStats>({ url: "/monitor/cache", method: "get" });
}

export function getCacheKeys() {
    return request<CacheEntry[]>({ url: "/monitor/cache/keys", method: "get" });
}

export function putCache(key: string, value: string) {
    return request<void>({
        url: `/monitor/cache/${key}`,
        method: "put",
        data: { value },
    });
}

export function deleteCacheKey(key: string) {
    return request<void>({ url: `/monitor/cache/${key}`, method: "delete" });
}

export function clearCache() {
    return request<void>({ url: "/monitor/cache", method: "delete" });
}
