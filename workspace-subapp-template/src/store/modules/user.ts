import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { getCurrentAppAuthorization, getCurrentUser, getToken } from "@/micro/auth";
import type { MicroUser } from "@/micro/contract";

/**
 * 用户状态。
 *
 * 与主应用最大的区别：这里没有 login、没有 setToken、没有请求用户信息的方法。
 * 账号体系由主应用统一持有，子应用只接收结果。
 *
 * 也不做持久化。原因有两个：
 * 1. 用户信息来自主应用注入，持久化会留下过期副本
 * 2. 多个子应用共用一个域名下的 localStorage，容易互相覆盖
 */
export const useUserStore = defineStore("user", () => {
    const user = ref<MicroUser>({ username: "" });
    const token = ref("");
    const roles = ref<string[]>([]);
    const permissions = ref<string[]>([]);
    let authorizationSequence = 0;

    /** 是否已登录。判断依据是 token 是否可获取，不依赖本地状态 */
    const isLogin = computed(() => token.value.length > 0);

    /** 显示名称，优先用真实姓名 */
    const displayName = computed(() => user.value.realName || user.value.username || "未登录");

    /**
     * 用主应用注入的信息初始化。
     * 每次挂载都要调用一次，保证切账号后拿到的是新身份。
     */
    function applyHostSession(options: {
        user?: MicroUser;
        roles?: string[];
        permissions?: string[];
    }): void {
        authorizationSequence += 1;
        token.value = getToken();
        user.value = options.user ?? getCurrentUser();
        roles.value = options.roles ?? [];
        permissions.value = options.permissions ?? [];
    }

    /** 从宿主鉴权桥刷新中央授权，忽略卸载或会话切换前的旧响应 */
    async function refreshAuthorization(): Promise<void> {
        const request = getCurrentAppAuthorization();
        if (!request) {
            return;
        }
        const sequence = ++authorizationSequence;
        try {
            const authorization = await request;
            if (sequence !== authorizationSequence) {
                return;
            }
            roles.value = [...authorization.roles];
            permissions.value = [...authorization.permissions];
        } catch (error) {
            if (sequence === authorizationSequence) {
                roles.value = [];
                permissions.value = [];
            }
            throw error;
        }
    }

    /** 只刷新 token。主应用续期后调用 */
    function refreshToken(): void {
        token.value = getToken();
    }

    /** 判断是否具备某个角色 */
    function hasRole(code: string): boolean {
        return roles.value.includes(code);
    }

    /** 判断是否具备某个权限码 */
    function hasPermission(code: string): boolean {
        return permissions.value.includes(code);
    }

    return {
        user,
        token,
        roles,
        permissions,
        isLogin,
        displayName,
        applyHostSession,
        refreshAuthorization,
        refreshToken,
        hasRole,
        hasPermission,
    };
});
