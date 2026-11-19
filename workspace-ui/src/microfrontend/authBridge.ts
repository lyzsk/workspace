import router from "@/router";
import { getMyAppAuthorization } from "@/api/system/app-grant";
import {
    useUserStore,
    type AuthSnapshot,
    type SessionEndReason,
} from "@/store/modules/user";

export interface HostUserSummary {
    id: string;
    username: string;
    realName?: string;
}

export interface HostAuthSnapshot extends Omit<AuthSnapshot, "user"> {
    user: HostUserSummary | null;
}

function userSummary(): HostUserSummary | null {
    const user = useUserStore().user;
    if (!user) {
        return null;
    }
    return {
        id: user.id,
        username: user.username,
        realName: user.realName,
    };
}

export const authBridge = {
    getToken(): string | null {
        return useUserStore().token || null;
    },
    getUser(): HostUserSummary | null {
        return userSummary();
    },
    onTokenChange(callback: (token: string | null) => void) {
        return useUserStore().onTokenChange(callback);
    },
    onAuthChange(callback: (snapshot: HostAuthSnapshot) => void) {
        return useUserStore().onAuthChange((snapshot) => {
            callback({
                ...snapshot,
                user: userSummary(),
            });
        });
    },
    async requestRefresh(): Promise<void> {
        await useUserStore().refresh();
    },
    async logout(reason: SessionEndReason = "microfrontend"): Promise<void> {
        await useUserStore().logout(reason);
    },
    navigate(path: string): void {
        void router.push(path);
    },
    onSessionExpired(callback: (reason: SessionEndReason) => void) {
        return useUserStore().onSessionExpired(callback);
    },
    getAppAuthorization(appCode: string) {
        return getMyAppAuthorization(appCode);
    },
};

export type AuthBridge = typeof authBridge;
export type ScopedAuthBridge = Omit<AuthBridge, "getAppAuthorization"> & {
    getAppAuthorization: () => ReturnType<typeof getMyAppAuthorization>;
};

export function createScopedAuthBridge(appCode: string): ScopedAuthBridge {
    return {
        ...authBridge,
        getAppAuthorization: () => getMyAppAuthorization(appCode),
    };
}
