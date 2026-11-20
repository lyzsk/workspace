import { createRouter, createMemoryHistory, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { emitAppEvent } from "@/micro/events";

/**
 * 路由工厂。
 *
 * 两种模式用不同的 history，这是本模板最关键的设计之一：
 *
 * - 微前端模式用 memory history：子应用完全不碰 window.location。
 *   因为同一时刻页面上只有一条地址栏，多个子应用都写 hash 会互相污染，
 *   切走再回来会停在别人的页面上。地址栏统一由主应用维护。
 *
 * - 独立模式用 web history：没有宿主，靠自己管理地址栏，刷新也能恢复。
 *
 * 因为 history 实例在 createRouter 时就固定了，所以必须在拿到宿主属性之后再创建路由。
 * 这也是 micro.ts 里要先 setMicroContext、再动态 import 应用模块的原因。
 */

const routes: RouteRecordRaw[] = [
    {
        path: "/",
        redirect: "/equipment",
    },
    {
        path: "/equipment",
        name: "Equipment",
        component: () => import("@/views/equipment/index.vue"),
        meta: { title: "设备清单" },
    },
    {
        path: "/ledger",
        name: "Ledger",
        component: () => import("@/views/ledger/index.vue"),
        meta: { title: "设备台账" },
    },
    {
        path: "/maintenance",
        name: "Maintenance",
        component: () => import("@/views/maintenance/index.vue"),
        meta: { title: "保养记录" },
    },
    {
        path: "/:pathMatch(.*)*",
        name: "NotFound",
        component: () => import("@/views/equipment/index.vue"),
        meta: { title: "设备清单" },
    },
];

export function createSubAppRouter(mode: "micro" | "standalone", initialPath?: string) {
    let history;

    if (mode === "micro") {
        const memoryHistory = createMemoryHistory();
        /**
         * 把宿主地址里的子路径写进初始位置。
         *
         * 必须在 createRouter 之前调用：此时 history 还没被 router 接管，
         * 替换只是设置初始位置，不会触发导航。
         * 如果在 createRouter 之后再跳，页面会先渲染一次首页再跳走，出现闪烁。
         */
        if (initialPath && initialPath.startsWith("/")) {
            memoryHistory.replace(initialPath);
        }
        history = memoryHistory;
    } else {
        history = createWebHistory(import.meta.env.VITE_BASE_URL || "/");
    }

    const router = createRouter({ history, routes });

    /**
     * 路由变化后通知主应用，让主应用同步地址栏与面包屑。
     *
     * 只在微前端模式下需要。独立运行没有宿主要接这个事件。
     */
    router.afterEach((to) => {
        if (mode !== "micro") {
            return;
        }
        // 带上页面标题，宿主据此渲染面包屑末级
        emitAppEvent("app.route.changed", {
            path: to.fullPath,
            title: typeof to.meta.title === "string" ? to.meta.title : undefined,
        });
    });

    return router;
}
