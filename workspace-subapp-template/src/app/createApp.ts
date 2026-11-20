import { createApp as createVueApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import "element-plus/theme-chalk/dark/css-vars.css";
import "@/styles/index.scss";
import App from "@/App.vue";
import { createSubAppRouter } from "@/router";
import { useUserStore } from "@/store/modules/user";
import { applyTheme } from "@/micro/theme";
import { emitAppEvent, onHostEvent } from "@/micro/events";
import type { MicroAppProps } from "@/micro/contract";

/**
 * 应用工厂。
 *
 * 独立运行和微前端挂载都走这里，两者的差异只体现在传入的参数上：
 * - mode 决定路由用哪种 history
 * - props 决定用户身份、主题、接口来源
 *
 * 把差异收敛到一个入口，好处是业务代码永远不用判断「我现在是不是在微前端里」。
 */

export interface SubAppOptions {
    /** 宿主注入的属性。独立运行时由 main.ts 造一份等价的 mock */
    props: MicroAppProps;
    /** 运行模式 */
    mode: "micro" | "standalone";
    /** 挂载容器。子应用所有 DOM 都在这个容器内 */
    container: HTMLElement;
}

export interface SubAppInstance {
    /** 卸载应用并清理所有监听器。必须由调用方在适当时机执行 */
    destroy: () => void;
}

/**
 * 解析真正的挂载点。
 *
 * 被 qiankun 挂载时，容器里通常已经有子应用 HTML 中的 #app 节点；
 * 直接用 container 挂载会和外层容器嵌套两层，样式作用域容易算错。
 * 所以优先复用容器里的 #app，没有才自己建一个。
 */
function resolveMountTarget(container: HTMLElement): HTMLElement {
    const existed = container.querySelector<HTMLElement>("#app");
    if (existed) {
        return existed;
    }

    const created = document.createElement("div");
    created.id = "app";
    container.appendChild(created);
    return created;
}

export function createSubApp(options: SubAppOptions): SubAppInstance {
    const { props, mode, container } = options;
    const mountTarget = resolveMountTarget(container);

    const app = createVueApp(App);
    const pinia = createPinia();
    app.use(pinia);

    // 路由必须在 setMicroContext 之后创建，否则会拿错 history 实例
    // 传入宿主给出的子路径，让子应用直接渲染对应页面，避免先闪首页
    const router = createSubAppRouter(mode, props.initialPath);
    app.use(router);

    // z-index 比主应用低一档，避免子应用弹层盖住主应用的全局提示与对话框
    app.use(ElementPlus, { zIndex: 2000 });

    // 用户身份来自主应用注入；中央角色与权限通过宿主鉴权桥单独加载
    const userStore = useUserStore(pinia);
    userStore.applyHostSession({
        user: props.user,
        roles: mode === "standalone" ? ["SUPER_ADMIN"] : [],
    });
    if (mode === "micro") {
        void userStore.refreshAuthorization().catch((error) => {
            console.error("[micro-subapp-template] 加载应用授权失败", error);
        });
    }

    // 主题需要在挂载前应用，否则首帧会闪一下浅色
    applyTheme(props.theme ?? "light");

    // 监听宿主下发的运行时变更。返回的取消函数在 destroy 里执行
    const offHostEvent = onHostEvent((detail) => {
        if (detail.type === "host.route.changed") {
            /**
             * 宿主的地址栏变了，子应用跟着换页。
             *
             * 主应用点左侧菜单时，宿主路由参数变化但子应用不会重新挂载，
             * 必须靠这个事件驱动子应用内部跳转。
             * 比对一下当前路径，避免宿主回发同一条地址导致重复跳转。
             */
            const target = detail.path;
            if (target && target !== router.currentRoute.value.fullPath) {
                void router.replace(target);
            }
            return;
        }
        if (detail.type === "host.theme.changed") {
            applyTheme(detail.theme ?? "light");
            return;
        }
        if (detail.type === "host.session.expired" || detail.type === "host.logout") {
            // 会话结束，清掉内存中的用户信息即可，跳转由主应用负责
            userStore.applyHostSession({ user: { username: "" } });
        }
    });

    /**
     * 未捕获异常上报。
     *
     * 子应用渲染出错时，宿主界面上只会看到一块空白区域，排查全靠翻控制台。
     * 把错误通过事件总线报出去，宿主能直接显示「子应用运行异常」并给出重试入口。
     */
    app.config.errorHandler = (err) => {
        const message = err instanceof Error ? err.message : String(err);
        console.error("[micro-subapp-template] 未捕获异常", err);
        emitAppEvent("app.error", { message });
    };

    app.mount(mountTarget);

    let destroyed = false;

    return {
        destroy() {
            if (destroyed) {
                return;
            }
            destroyed = true;
            offHostEvent();
            app.unmount();
        },
    };
}
