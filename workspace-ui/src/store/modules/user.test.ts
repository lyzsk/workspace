import { beforeEach, afterEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import {
    getSessionUser,
    isAuthUnauthorized,
    refreshRequest,
} from "@/api/authTransport";
import { useUserStore } from "@/store/modules/user";
import type { LoginResult, UserInfo } from "@/types/api";

vi.mock("@/api/authTransport", () => ({
    loginRequest: vi.fn(),
    getSessionUser: vi.fn(),
    refreshRequest: vi.fn(),
    logoutRequest: vi.fn(),
    isAuthUnauthorized: vi.fn(() => false),
}));

function user(id: string, username: string): UserInfo {
    return {
        id,
        username,
        realName: username,
        gender: 0,
        roles: [],
        permissions: [],
    };
}

function result(token: string, data: UserInfo): LoginResult {
    return { token, user: data };
}

function deferred<T>() {
    let resolve!: (value: T) => void;
    let reject!: (reason?: unknown) => void;
    const promise = new Promise<T>((resolvePromise, rejectPromise) => {
        resolve = resolvePromise;
        reject = rejectPromise;
    });
    return { promise, resolve, reject };
}

beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal("BroadcastChannel", undefined);
    vi.stubGlobal("window", {
        location: { pathname: "/" },
        localStorage: { removeItem: vi.fn() },
        dispatchEvent: vi.fn(),
    });
    setActivePinia(createPinia());
});

afterEach(() => {
    vi.unstubAllGlobals();
});

describe("user session", () => {
    it("shares one refresh request and emits the new token once", async () => {
        const store = useUserStore();
        const pending = deferred<LoginResult>();
        const listener = vi.fn();
        store.token = "old-token";
        store.user = user("1", "old");
        store.status = "authenticated";
        vi.mocked(refreshRequest).mockReturnValue(pending.promise);
        const off = store.onTokenChange(listener);

        const first = store.refresh();
        const second = store.refresh();
        expect(refreshRequest).toHaveBeenCalledTimes(1);

        pending.resolve(result("new-token", user("1", "new")));
        await Promise.all([first, second]);
        expect(store.token).toBe("new-token");
        expect(store.user?.username).toBe("new");
        expect(listener).toHaveBeenCalledTimes(1);
        expect(listener).toHaveBeenCalledWith("new-token");
        off();
    });

    it("does not let an old refresh response overwrite a new session", async () => {
        const store = useUserStore();
        const pending = deferred<LoginResult>();
        store.token = "old-token";
        store.user = user("1", "old");
        store.status = "authenticated";
        vi.mocked(refreshRequest).mockReturnValue(pending.promise);

        const refreshing = store.refresh();
        store.sessionEpoch += 1;
        store.token = "new-session-token";
        store.user = user("2", "new-session");
        store.status = "authenticated";
        pending.resolve(result("stale-token", user("1", "stale")));

        await expect(refreshing).rejects.toThrow("STALE_SESSION");
        expect(store.token).toBe("new-session-token");
        expect(store.user?.username).toBe("new-session");
    });

    it("does not expire the session when refresh infrastructure is unavailable", async () => {
        const store = useUserStore();
        store.token = "old-token";
        store.user = user("1", "old");
        store.status = "authenticated";
        vi.mocked(refreshRequest).mockRejectedValue(new Error("AUTH_UNAVAILABLE"));
        vi.mocked(isAuthUnauthorized).mockReturnValue(false);

        await expect(store.refresh()).rejects.toThrow("AUTH_UNAVAILABLE");
        expect(store.token).toBe("old-token");
        expect(store.status).toBe("authenticated");
    });

    it("continues with the restored local session after a temporary profile failure", async () => {
        const store = useUserStore();
        store.token = "old-token";
        store.user = user("1", "old");
        store.status = "anonymous";
        vi.mocked(getSessionUser).mockRejectedValue(new Error("NETWORK_UNAVAILABLE"));
        vi.mocked(isAuthUnauthorized).mockReturnValue(false);

        await expect(store.restore()).resolves.toBeUndefined();
        expect(store.token).toBe("old-token");
        expect(store.status).toBe("authenticated");
    });

    it("does not let an old restore response overwrite a new session", async () => {
        const store = useUserStore();
        const pending = deferred<UserInfo>();
        store.token = "old-token";
        store.user = user("1", "old");
        vi.mocked(getSessionUser).mockReturnValue(pending.promise);

        const restoring = store.restore();
        store.sessionEpoch += 1;
        store.token = "new-session-token";
        store.user = user("2", "new-session");
        store.status = "authenticated";
        pending.resolve(user("1", "stale"));

        await expect(restoring).rejects.toThrow("STALE_SESSION");
        expect(store.token).toBe("new-session-token");
        expect(store.user?.username).toBe("new-session");
    });
});
