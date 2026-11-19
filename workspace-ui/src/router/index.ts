import {
    createRouter,
    createWebHistory,
    type RouteRecordRaw,
} from "vue-router";
import NProgress from "nprogress";
import "nprogress/nprogress.css";
import { usePermissionStore } from "@/store/modules/permission";
import { useUserStore } from "@/store/modules/user";

NProgress.configure({ showSpinner: false, trickleSpeed: 120 });

const routes: RouteRecordRaw[] = [
    {
        path: "/login",
        name: "Login",
        component: () => import("@/views/login/index.vue"),
        meta: { title: "login" },
    },
    {
        path: "/",
        name: "Layout",
        component: () => import("@/views/index/index.vue"),
        redirect: "/dashboard",
        children: [
            {
                path: "dashboard",
                name: "Dashboard",
                component: () => import("@/views/dashboard/index.vue"),
                meta: { title: "menu.dashboard" },
            },
            {
                path: "403",
                name: "Exception403",
                component: () => import("@/views/exception/403/index.vue"),
                meta: { title: "403" },
            },
            {
                path: "404",
                name: "Exception404",
                component: () => import("@/views/exception/404/index.vue"),
                meta: { title: "404" },
            },
            {
                path: "500",
                name: "Exception500",
                component: () => import("@/views/exception/500/index.vue"),
                meta: { title: "500" },
            },
            {
                path: "result/success",
                name: "ResultSuccess",
                component: () => import("@/views/result/success/index.vue"),
                meta: { title: "成功页" },
            },
            {
                path: "result/fail",
                name: "ResultFail",
                component: () => import("@/views/result/fail/index.vue"),
                meta: { title: "失败页" },
            },
            {
                path: "system/user-center",
                name: "UserCenter",
                component: () => import("@/views/system/user-center/index.vue"),
                meta: { title: "个人中心" },
            },
            {
                path: ":pathMatch(.*)*",
                name: "NotFound",
                component: () => import("@/views/exception/404/index.vue"),
                meta: { title: "404" },
            },
        ],
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

router.beforeEach(async (to) => {
    NProgress.start();
    const userStore = useUserStore();
    const permissionStore = usePermissionStore();
    await userStore.restore();

    if (to.name !== "Login" && !userStore.isLogin) {
        return { name: "Login", query: { redirect: to.fullPath } };
    }

    if (to.name === "Login" && userStore.isLogin) {
        return { path: "/" };
    }

    if (userStore.isLogin && !permissionStore.loaded) {
        try {
            await permissionStore.loadAndBuild(router);
            return { path: to.fullPath, replace: true };
        } catch (error) {
            if (!userStore.isLogin) {
                return {
                    name: "Login",
                    query: { redirect: to.fullPath, reason: "expired" },
                };
            }
            throw error;
        }
    }

    if (to.name === "NotFound") {
        return { path: permissionStore.homePath, replace: true };
    }

    const required = to.meta.permission;
    if (required && !userStore.hasPerm(String(required))) {
        const home = permissionStore.homePath;
        return { path: to.path === home ? "/dashboard" : home, replace: true };
    }

    return true;
});

router.afterEach(() => {
    NProgress.done();
});

router.onError(() => {
    NProgress.done();
});

export default router;
