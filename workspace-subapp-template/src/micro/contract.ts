/**
 * 子应用与主应用之间的唯一契约。
 *
 * 这里的每个字段、每个事件名都必须与主应用
 * `workspace-ui/src/utils/qiankun.ts` 的 props、
 * `workspace-ui/src/microfrontend/authBridge.ts` 的鉴权方法、
 * `workspace-ui/src/utils/microFrontendEvents.ts` 的事件定义保持一致。
 *
 * 修改本文件前，先确认主应用侧同步修改，否则会出现「属性取不到」「事件没人接」这类
 * 静默失败 —— 这类问题不会报错，只会表现为某个功能不生效。
 */

/** 主应用挂载子应用时注入的属性 */
export interface MicroAppProps {
    /** qiankun 传入的挂载容器。子应用所有 DOM 都必须渲染在这个容器内 */
    container?: HTMLElement;
    /** 主应用自研加载器传入的挂载容器。与 container 二选一，兼容两种宿主实现 */
    domElement?: HTMLElement;
    /** 应用路由前缀，例如 /apps/ies。子应用内的跳转、面包屑都基于它 */
    basePath: string;
    /**
     * 子应用内部初始路径，例如 /equipment。
     * 宿主把地址栏里的「前缀之后的部分」传进来，子应用据此直接渲染对应页面，
     * 刷新或从菜单直达时不会先闪一下首页。
     */
    initialPath?: string;
    /** 应用编码，与主应用 sys_app.code 一致。用于事件过滤 */
    appCode: string;
    /** 运行模式标记。宿主传入固定值 micro */
    mode?: "micro" | "standalone";
    /** 当前主题。主应用切换主题后会通过 host.theme.changed 事件再次推送 */
    theme?: ThemeMode;
    /** 当前语言，例如 zh-CN */
    locale?: string;
    /** 当前登录用户。子应用不自行请求用户信息 */
    user?: MicroUser;
    /**
     * 获取主应用 token 的方法。
     * 子应用不缓存 token，每次发请求前调用，保证与主应用登录态实时一致。
     */
    getToken?: () => string | null;
    /** 请求主应用切换路由 */
    navigate?: (path: string) => void;
    /** 会话已过期时通知主应用 */
    onSessionExpired?: () => void;
    /** 按当前应用读取中央角色、权限与菜单授权 */
    getAppAuthorization?: () => Promise<MicroAppAuthorization>;
}

export interface MicroAppAuthorization {
    accessible: boolean;
    roles: string[];
    permissions: string[];
}

export type ThemeMode = "light" | "dark";

export interface MicroUser {
    id?: string;
    username: string;
    realName?: string;
}

/** 事件总线名称，与主应用完全一致 */
export const MICRO_EVENT_NAME = "workspace:micro-frontend";

/** 事件版本号。主应用只处理 version 为 1 的事件 */
export const MICRO_EVENT_VERSION = 1;

/**
 * 事件类型。
 *
 * 前半部分是宿主发给子应用（host.*），后半部分是子应用发给宿主（app.*）。
 * 子应用只需要发送 app.* 中的事件。
 */
export type MicroEventType =
    | "host.app.entered"
    | "host.app.left"
    | "host.app.error"
    | "host.theme.changed"
    | "host.locale.changed"
    | "host.session.expired"
    | "host.logout"
    | "host.route.changed"
    | "app.ready"
    | "app.route.changed"
    | "app.theme.changed"
    | "app.logout"
    | "app.session.expired"
    | "app.auth.failed"
    | "app.error";

/** 事件载荷结构 */
export interface MicroEventDetail {
    version: number;
    type: MicroEventType;
    appCode: string;
    path: string;
    time: string;
    message?: string;
    theme?: ThemeMode;
    locale?: string;
    /** 当前页面标题，宿主用它渲染面包屑末级 */
    title?: string;
    /** 完整标题链，主应用侧边栏与面包屑多级显示时使用 */
    titles?: string[];
}
