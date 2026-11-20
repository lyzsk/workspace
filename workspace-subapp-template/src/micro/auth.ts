import { AppConfig } from "@/config";
import { getMicroContext, isMicroMode, navigateTo } from "./context";
import { emitAppEvent } from "./events";
import type { MicroAppAuthorization, MicroUser } from "./contract";

/**
 * 鉴权。
 *
 * 本模板不实现登录：账号体系由主应用统一持有。
 * 子应用只做三件事：
 * 1. 发请求前从主应用现取 token
 * 2. 收到 401 时上报，由主应用决定跳登录页还是弹提示
 * 3. 主动登出时通知主应用
 *
 * 因此子应用不需要登录页、不需要刷新 token、不需要处理密码。
 */

const STANDALONE_TOKEN_KEY = "micro-template-token";

/**
 * 取当前 token。
 *
 * 微前端模式下每次调用都会向主应用取最新值，不做缓存。
 * 这样做是为了避免主应用换了账号、刷新了 token 之后子应用还拿着旧 token 发请求。
 */
export function getToken(): string {
    if (isMicroMode()) {
        return getMicroContext()?.getToken?.() ?? "";
    }

    // 独立运行：开发期使用占位 token，真实联调时手动写入 localStorage
    return window.localStorage.getItem(STANDALONE_TOKEN_KEY) || AppConfig.micro.mockToken;
}

/** 取当前用户。微前端由主应用注入，独立运行使用本地占位身份 */
export function getCurrentUser(): MicroUser {
    if (isMicroMode()) {
        return getMicroContext()?.user ?? { username: "unknown" };
    }
    return { username: "local", realName: "本地调试" };
}

/** 通过宿主鉴权桥读取当前应用的中央授权，不在子应用持久化 */
export function getCurrentAppAuthorization(): Promise<MicroAppAuthorization> | null {
    if (!isMicroMode()) {
        return null;
    }
    return getMicroContext()?.getAppAuthorization?.() ?? null;
}

/**
 * 上报会话过期。
 *
 * 注意：子应用不弹窗、不跳转。跳转逻辑由主应用统一处理，
 * 否则多个子应用各自跳一次，用户会看到地址栏来回闪。
 */
export function reportSessionExpired(): void {
    if (isMicroMode()) {
        getMicroContext()?.onSessionExpired?.();
        emitAppEvent("app.session.expired", { message: "会话已过期" });
        return;
    }

    // 独立运行没有宿主兜底，只能提示用户自行处理
    console.warn("[micro-subapp-template] 会话已过期，独立运行模式请重新写入 token");
}

/** 上报鉴权失败（401 之外的权限问题，例如 403） */
export function reportAuthFailed(message: string): void {
    emitAppEvent("app.auth.failed", { message });
}

/**
 * 主动登出。
 * 微前端模式下由主应用执行真正的登出，子应用只发信号。
 */
export function logout(): void {
    if (isMicroMode()) {
        emitAppEvent("app.logout", {});
        return;
    }
    window.localStorage.removeItem(STANDALONE_TOKEN_KEY);
    navigateTo("/login");
}
