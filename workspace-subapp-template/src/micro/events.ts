import {
    getAppCode,
    getBasePath,
    isMicroMode,
} from "./context";
import {
    MICRO_EVENT_NAME,
    MICRO_EVENT_VERSION,
    type MicroEventDetail,
    type MicroEventType,
    type ThemeMode,
} from "./contract";

/**
 * 事件总线的子应用侧实现。
 *
 * 主应用与子应用通过 window 上的 CustomEvent 通信，不共享任何 JS 对象。
 * 这样做的好处是：子应用被沙箱隔离、被换成不同构建工具时，通信层不需要改。
 */

/** 子应用允许发送的事件类型。类型收窄可以防止业务代码乱发事件 */
type AppEventType =
    | "app.ready"
    | "app.route.changed"
    | "app.theme.changed"
    | "app.logout"
    | "app.session.expired"
    | "app.auth.failed"
    | "app.error";

interface AppEventExtra {
    message?: string;
    theme?: ThemeMode;
    locale?: string;
    /** 覆盖默认路径，一般传子应用内部路由的完整地址 */
    path?: string;
    /** 当前页面标题，宿主渲染面包屑末级时使用 */
    title?: string;
    /** 完整标题链，多级面包屑时使用 */
    titles?: string[];
}

/** 取当前页面路径，作为事件的默认 path */
function currentPath(): string {
    return `${getBasePath()}${window.location.hash || ""}`;
}

/**
 * 向主应用发送事件。
 *
 * 独立运行时不做任何事：没有宿主要接这些事件，发出去只是噪音。
 */
export function emitAppEvent(type: AppEventType, extra: AppEventExtra = {}): void {
    if (!isMicroMode()) {
        return;
    }

    const detail: MicroEventDetail = {
        version: MICRO_EVENT_VERSION,
        type: type as MicroEventType,
        appCode: getAppCode(),
        path: extra.path ?? currentPath(),
        time: new Date().toISOString(),
        message: extra.message,
        theme: extra.theme,
        locale: extra.locale,
    };

    window.dispatchEvent(
        new CustomEvent<MicroEventDetail>(MICRO_EVENT_NAME, { detail }),
    );
}

/** 宿主事件回调，已经过滤掉不属于本应用的事件 */
export interface HostEventHandler {
    (detail: MicroEventDetail): void;
}

/**
 * 监听主应用发来的事件。
 *
 * 返回取消监听的函数，调用方必须在卸载时执行，否则子应用反复挂载会累积监听器。
 */
export function onHostEvent(handler: HostEventHandler): () => void {
    const appCode = getAppCode();

    const listener = (event: Event) => {
        const detail = (event as CustomEvent<MicroEventDetail>).detail;
        // 版本不符或不是发给自己的事件，直接忽略
        if (!detail || detail.version !== MICRO_EVENT_VERSION) {
            return;
        }
        // appCode 为 * 表示广播，所有子应用都要接收；否则只接收指向自己的事件
        const isBroadcast = detail.appCode === "*";
        if (detail.appCode && !isBroadcast && detail.appCode !== appCode) {
            return;
        }
        handler(detail);
    };

    window.addEventListener(MICRO_EVENT_NAME, listener);
    return () => window.removeEventListener(MICRO_EVENT_NAME, listener);
}
