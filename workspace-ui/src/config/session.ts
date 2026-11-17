export const SessionConfig = {
    idleTimeoutMs: 30 * 60 * 1000,
    idleWarningMs: 60 * 1000,
    idleCheckIntervalMs: 15 * 1000,
    heartbeatThrottleMs: 60 * 1000,
    draftTtlMs: 24 * 60 * 60 * 1000,
    draftAutosaveDelayMs: 600,
    dataRefreshDebounceMs: 300,
} as const;
