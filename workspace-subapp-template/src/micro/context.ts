import type { MicroAppProps } from "./contract";

/**
 * 运行上下文。
 *
 * 主应用注入的属性只在挂载时到达，而 router、store、axios 在模块加载时就要用到它们。
 * 因此这里用一个模块级的单一变量持有，保证任何位置都能同步取到，不需要层层传参。
 *
 * 重要约束：必须在创建 Vue 应用实例之前调用 setMicroContext，
 * 否则 router 会按「独立运行」模式初始化，拿错 history 实例。
 */

let currentProps: MicroAppProps | null = null;

/** 写入宿主注入的属性。由 micro.ts 在挂载时调用 */
export function setMicroContext(props: MicroAppProps): void {
    currentProps = props;
}

/** 读取宿主注入的属性。未挂载时返回 null */
export function getMicroContext(): MicroAppProps | null {
    return currentProps;
}

/**
 * 当前运行模式。
 *
 * 不能只看「上下文有没有值」：独立运行同样会写一份等价的属性到上下文里，
 * 两种情况都有值，靠有没有值区分不出来，必须读 props.mode。
 */
export function getRunMode(): "micro" | "standalone" {
    if (!currentProps) {
        return "standalone";
    }
    return currentProps.mode === "standalone" ? "standalone" : "micro";
}

/** 是否运行在微前端容器内 */
export function isMicroMode(): boolean {
    return getRunMode() === "micro";
}

/**
 * 取挂载容器。
 * qiankun 用 container，主应用自研加载器用 domElement，两者都要兼容。
 */
export function getContainer(): HTMLElement | null {
    if (!currentProps) {
        return null;
    }
    return currentProps.container ?? currentProps.domElement ?? null;
}

/** 取应用编码。独立运行时回退到构建期配置 */
export function getAppCode(): string {
    return currentProps?.appCode || import.meta.env.VITE_APP_CODE || "subapp";
}

/** 取路由前缀。独立运行时回退到构建期配置 */
export function getBasePath(): string {
    return currentProps?.basePath || import.meta.env.VITE_APP_BASE_PATH || "/";
}

/**
 * 请求主应用跳转。
 * 微前端模式下交给宿主，独立模式下走浏览器地址栏，
 * 保证业务代码里只有一种写法。
 */
export function navigateTo(path: string): void {
    if (currentProps?.navigate) {
        currentProps.navigate(path);
        return;
    }
    window.location.href = path;
}
