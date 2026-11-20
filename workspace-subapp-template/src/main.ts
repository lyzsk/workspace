import { AppConfig } from "./config";
import { setMicroContext } from "./micro/context";
import type { MicroAppProps } from "./micro/contract";

/**
 * 应用入口。
 *
 * 同一个入口文件承担两种职责，这是 qiankun 对 Vite 子应用的要求：
 * - 被主应用加载时：导出生命周期，由宿主调用，本文件不主动挂载
 * - 独立运行时：自己造一份等价属性，直接挂载
 *
 * 两条路径最终都走 createSubApp，业务代码无需关心当前在哪一种模式里。
 */

// 生命周期必须从入口模块导出，qiankun 依据这里的导出判断这是不是一个子应用
export { bootstrap, mount, unmount, update } from "./micro";

/** 独立运行时使用的属性，字段与宿主注入的完全一致 */
const standaloneProps: MicroAppProps = {
    appCode: import.meta.env.VITE_APP_CODE || AppConfig.micro.appCode,
    basePath: import.meta.env.VITE_APP_BASE_PATH || AppConfig.micro.basePath,
    mode: "standalone",
    theme: "light",
    locale: "zh-CN",
    user: { username: "local", realName: "本地调试" },
    getToken: () => AppConfig.micro.mockToken,
    navigate: (path: string) => {
        window.location.href = path;
    },
    onSessionExpired: () => {
        console.warn("[micro-subapp-template] 会话已过期");
    },
};

/**
 * 独立运行时的启动逻辑。
 *
 * 这里用动态加载而不是文件顶部静态引入，原因有两个：
 * 1. 与 micro.ts 保持一致，应用模块只在真正要渲染时才加载
 * 2. 避免构建工具把应用模块拆成主 chunk，微前端模式下白白解析一遍
 */
async function startStandalone(): Promise<void> {
    const container = document.getElementById("app");
    if (!container) {
        throw new Error("独立运行失败：页面缺少 #app 容器");
    }

    setMicroContext(standaloneProps);

    const { createSubApp } = await import("./app/createApp");
    createSubApp({ props: standaloneProps, mode: "standalone", container });
}

/**
 * 是否需要自行启动。
 *
 * 判断依据是 qiankun 注入的全局标记。另外留了一个 ?standalone=1 的强制开关：
 * 万一宿主实现变化导致标记缺失，加这个参数就能本地调试，不至于卡住。
 */
const forcedStandalone = new URLSearchParams(window.location.search).has("standalone");
const poweredByHost = Boolean(window.__POWERED_BY_QIANKUN__);

if (forcedStandalone || !poweredByHost) {
    startStandalone().catch((error) => {
        console.error("[micro-subapp-template] 独立启动失败", error);
    });
}
