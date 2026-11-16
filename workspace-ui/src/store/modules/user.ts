import { computed, ref } from "vue";
import { defineStore } from "pinia";
import type { LoginResult, UserInfo } from "@/types/api";

export const useUserStore = defineStore(
    "user",
    () => {
        const token = ref("");
        const user = ref<UserInfo | null>(null);
        const isLogin = computed(() => token.value.length > 0);
        const roles = computed(() => user.value?.roles ?? []);
        const permissions = computed(() => user.value?.permissions ?? []);

        function setLogin(result: LoginResult) {
            token.value = result.token;
            user.value = result.user;
        }

        function setUser(data: UserInfo) {
            user.value = data;
        }

        function hasPerm(code: string) {
            return (
                roles.value.includes("SUPER_ADMIN") ||
                permissions.value.includes(code)
            );
        }

        function logout() {
            token.value = "";
            user.value = null;
        }

        if (typeof window !== "undefined") {
            window.addEventListener("storage", (event) => {
                if (event.key !== "workspace-user" || !event.newValue) {
                    return;
                }
                try {
                    const data = JSON.parse(event.newValue) as {
                        token?: string;
                        user?: UserInfo | null;
                    };
                    if (
                        data.token !== undefined &&
                        data.token !== token.value
                    ) {
                        token.value = data.token;
                    }
                    if (
                        data.user !== undefined &&
                        JSON.stringify(data.user) !== JSON.stringify(user.value)
                    ) {
                        user.value = data.user;
                    }
                } catch {
                    // ignore malformed storage payloads
                }
            });
        }

        return {
            token,
            user,
            isLogin,
            roles,
            permissions,
            setLogin,
            setUser,
            hasPerm,
            logout,
        };
    },
    {
        persist: {
            key: "workspace-user",
        },
    },
);
