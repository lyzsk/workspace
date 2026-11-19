import { computed, ref, shallowRef } from "vue";
import { defineStore } from "pinia";
import {
    getSessionUser,
    isAuthUnauthorized,
    loginRequest,
    logoutRequest,
    refreshRequest,
} from "@/api/authTransport";
import type {
    LoginPayload,
    LoginResult,
    UserInfo,
} from "@/types/api";
import {
    clearDraftsForUser,
    markSessionEnding,
    resetSessionEnding,
    saveAllDrafts,
} from "@/utils/draft";
import { emitMicroFrontendEvent } from "@/utils/microFrontendEvents";

export type AuthStatus =
    | "anonymous"
    | "restoring"
    | "authenticated"
    | "refreshing"
    | "expired";

export type SessionEndReason =
    | "manual"
    | "microfrontend"
    | "password"
    | "expired"
    | "idle"
    | "remote";

export interface AuthSnapshot {
    status: AuthStatus;
    user: UserInfo | null;
    sessionEpoch: number;
    reason: SessionEndReason | null;
}

interface LogoutOptions {
    broadcast?: boolean;
}

const LOGOUT_CHANNEL = "workspace-session";

export const useUserStore = defineStore(
    "user",
    () => {
        const token = ref("");
        const user = ref<UserInfo | null>(null);
        const status = ref<AuthStatus>("anonymous");
        const sessionEpoch = ref(0);
        const endReason = ref<SessionEndReason | null>(null);
        const restorePromise = shallowRef<Promise<void> | null>(null);
        const refreshPromise = shallowRef<Promise<void> | null>(null);
        const logoutPromise = shallowRef<Promise<void> | null>(null);
        const isLogin = computed(() => token.value.length > 0);
        const roles = computed(() => user.value?.roles ?? []);
        const permissions = computed(() => user.value?.permissions ?? []);
        const tokenListeners = new Set<(value: string | null) => void>();
        const authListeners = new Set<(value: AuthSnapshot) => void>();
        const expiredListeners = new Set<
            (reason: SessionEndReason) => void
        >();
        let channel: BroadcastChannel | undefined;

        function snapshot(): AuthSnapshot {
            return {
                status: status.value,
                user: user.value,
                sessionEpoch: sessionEpoch.value,
                reason: endReason.value,
            };
        }

        function notifyToken(value: string | null) {
            tokenListeners.forEach((listener) => listener(value));
        }

        function notifyAuth() {
            const value = snapshot();
            authListeners.forEach((listener) => listener(value));
        }

        function applyLogin(result: LoginResult) {
            const previousToken = token.value;
            token.value = result.token;
            user.value = result.user;
            status.value = "authenticated";
            endReason.value = null;
            resetSessionEnding();
            if (previousToken !== result.token) {
                notifyToken(result.token || null);
            }
            notifyAuth();
        }

        function prepareDrafts(reason: SessionEndReason) {
            if (reason === "expired" || reason === "idle") {
                markSessionEnding();
                saveAllDrafts();
                return;
            }
            clearDraftsForUser(user.value?.id);
        }

        function announceEnd(reason: SessionEndReason) {
            const path = window.location.pathname;
            if (reason === "expired" || reason === "idle") {
                emitMicroFrontendEvent("host.session.expired", {
                    appCode: "*",
                    path,
                });
                window.dispatchEvent(new Event("workspace:session-expired"));
                expiredListeners.forEach((listener) => listener(reason));
            } else {
                emitMicroFrontendEvent("host.logout", {
                    appCode: "*",
                    path,
                });
            }
        }

        function clearSession(reason: SessionEndReason) {
            const hadToken = token.value.length > 0;
            token.value = "";
            user.value = null;
            status.value =
                reason === "expired" || reason === "idle"
                    ? "expired"
                    : "anonymous";
            endReason.value = reason;
            if (hadToken) {
                notifyToken(null);
            }
            notifyAuth();
        }

        async function login(payload: LoginPayload) {
            if (logoutPromise.value) {
                await logoutPromise.value;
            }
            const epoch = ++sessionEpoch.value;
            restorePromise.value = null;
            refreshPromise.value = null;
            status.value = "restoring";
            endReason.value = null;
            try {
                const result = await loginRequest(payload);
                if (epoch !== sessionEpoch.value) {
                    throw new Error("STALE_SESSION");
                }
                applyLogin(result);
            } catch (error) {
                if (epoch === sessionEpoch.value) {
                    token.value = "";
                    user.value = null;
                    status.value = "anonymous";
                }
                throw error;
            }
        }

        function restore(): Promise<void> {
            if (restorePromise.value) {
                return restorePromise.value;
            }
            if (
                status.value === "authenticated" ||
                (!token.value &&
                    (status.value === "anonymous" ||
                        status.value === "expired"))
            ) {
                return Promise.resolve();
            }
            const epoch = sessionEpoch.value;
            const operation = (async () => {
                status.value = "restoring";
                try {
                    const data = await getSessionUser(token.value);
                    if (epoch !== sessionEpoch.value || !token.value) {
                        throw new Error("STALE_SESSION");
                    }
                    user.value = data;
                    status.value = "authenticated";
                    notifyAuth();
                } catch (error) {
                    if (error instanceof Error && error.message === "STALE_SESSION") {
                        throw error;
                    }
                    if (!isAuthUnauthorized(error)) {
                        if (epoch === sessionEpoch.value && token.value) {
                            status.value = "authenticated";
                        }
                        return;
                    }
                    if (epoch !== sessionEpoch.value) {
                        throw new Error("STALE_SESSION");
                    }
                    await refresh();
                }
            })();
            let pending: Promise<void>;
            pending = operation.finally(() => {
                if (restorePromise.value === pending) {
                    restorePromise.value = null;
                }
            });
            restorePromise.value = pending;
            return pending;
        }

        function refresh(): Promise<void> {
            if (refreshPromise.value) {
                return refreshPromise.value;
            }
            if (!token.value) {
                return Promise.reject(new Error("NO_ACTIVE_SESSION"));
            }
            const epoch = sessionEpoch.value;
            const previousStatus = status.value;
            status.value = "refreshing";
            const operation = (async () => {
                try {
                    const result = await refreshRequest(token.value);
                    if (epoch !== sessionEpoch.value || !token.value) {
                        throw new Error("STALE_SESSION");
                    }
                    applyLogin(result);
                } catch (error) {
                    if (
                        epoch === sessionEpoch.value &&
                        !(error instanceof Error && error.message === "STALE_SESSION") &&
                        isAuthUnauthorized(error)
                    ) {
                        await expire("expired");
                    }
                    throw error;
                } finally {
                    if (
                        epoch === sessionEpoch.value &&
                        status.value === "refreshing"
                    ) {
                        status.value = previousStatus;
                    }
                }
            })();
            let pending: Promise<void>;
            pending = operation.finally(() => {
                if (refreshPromise.value === pending) {
                    refreshPromise.value = null;
                }
            });
            refreshPromise.value = pending;
            return pending;
        }

        function logout(
            reason: SessionEndReason = "manual",
            options: LogoutOptions = {},
        ): Promise<void> {
            if (logoutPromise.value) {
                return logoutPromise.value;
            }
            const epoch = ++sessionEpoch.value;
            restorePromise.value = null;
            refreshPromise.value = null;
            const currentToken = token.value;
            const operation = (async () => {
                announceEnd(reason);
                prepareDrafts(reason);
                if (options.broadcast !== false) {
                    channel?.postMessage({ type: "logout", reason });
                }
                if (epoch === sessionEpoch.value) {
                    clearSession(reason);
                }
                try {
                    await logoutRequest(currentToken);
                } catch {
                }
            })();
            let pending: Promise<void>;
            pending = operation.finally(() => {
                if (logoutPromise.value === pending) {
                    logoutPromise.value = null;
                }
            });
            logoutPromise.value = pending;
            return pending;
        }

        function expire(reason: SessionEndReason = "expired"): Promise<void> {
            if (logoutPromise.value) {
                return logoutPromise.value;
            }
            if (!token.value && !user.value) {
                return Promise.resolve();
            }
            sessionEpoch.value += 1;
            restorePromise.value = null;
            refreshPromise.value = null;
            announceEnd(reason);
            prepareDrafts(reason);
            clearSession(reason);
            return Promise.resolve();
        }

        function setUser(data: UserInfo) {
            if (!token.value) {
                return;
            }
            user.value = data;
            notifyAuth();
        }

        function hasPerm(code: string) {
            return (
                roles.value.includes("SUPER_ADMIN") ||
                permissions.value.includes(code)
            );
        }

        function onTokenChange(callback: (value: string | null) => void) {
            tokenListeners.add(callback);
            return () => {
                tokenListeners.delete(callback);
            };
        }

        function onAuthChange(callback: (value: AuthSnapshot) => void) {
            authListeners.add(callback);
            return () => {
                authListeners.delete(callback);
            };
        }

        function onSessionExpired(
            callback: (reason: SessionEndReason) => void,
        ) {
            expiredListeners.add(callback);
            return () => {
                expiredListeners.delete(callback);
            };
        }

        if (typeof window !== "undefined") {
            window.localStorage.removeItem("workspace-user");
            if (typeof BroadcastChannel !== "undefined") {
                channel = new BroadcastChannel(LOGOUT_CHANNEL);
                channel.onmessage = (event) => {
                    if (event.data?.type === "logout") {
                        void expire(event.data?.reason === "idle" ? "idle" : "remote");
                    }
                };
            }
        }

        return {
            token,
            user,
            status,
            sessionEpoch,
            endReason,
            restorePromise,
            refreshPromise,
            logoutPromise,
            isLogin,
            roles,
            permissions,
            login,
            restore,
            refresh,
            logout,
            expire,
            setUser,
            hasPerm,
            onTokenChange,
            onAuthChange,
            onSessionExpired,
        };
    },
    {
        persist: {
            key: "workspace-user",
            storage: sessionStorage,
            pick: ["token", "user"],
        },
    },
);
