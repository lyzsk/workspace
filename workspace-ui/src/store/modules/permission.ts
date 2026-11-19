import { computed, ref } from "vue";
import { defineStore } from "pinia";
import type { Component } from "vue";
import type { RouteRecordRaw, Router } from "vue-router";
import { getMyMenus } from "@/api/system/menu";
import { useUserStore } from "@/store/modules/user";
import type { MenuNode } from "@/types/system";

type ViewModule = () => Promise<{ default: Component }>;

const viewModules = import.meta.glob<{ default: Component }>(
    "../../views/**/*.vue",
);

function flattenMenus(menus: MenuNode[]): MenuNode[] {
    const result: MenuNode[] = [];
    const walk = (items: MenuNode[]) => {
        for (const item of items) {
            result.push(item);
            if (item.children?.length) {
                walk(item.children);
            }
        }
    };
    walk(menus);
    return result;
}

function resolveView(component?: string): ViewModule | undefined {
    if (!component) {
        return undefined;
    }
    return viewModules[`../../views/${component}.vue`];
}

function firstMenuPath(menus: MenuNode[]): string | undefined {
    for (const menu of menus) {
        if (
            menu.menuType === "MENU" &&
            menu.path &&
            menu.hidden !== 0 &&
            resolveView(menu.component)
        ) {
            return menu.path;
        }
        if (menu.children?.length) {
            const child = firstMenuPath(menu.children);
            if (child) {
                return child;
            }
        }
    }
    return undefined;
}

export const usePermissionStore = defineStore("permission", () => {
    const menus = ref<MenuNode[]>([]);
    const loaded = ref(false);
    const removers: Array<() => void> = [];
    const homePath = computed(() => firstMenuPath(menus.value) ?? "/dashboard");
    let loadPromise: Promise<void> | null = null;
    let snapshotGeneration = 0;

    function buildRoutes(tree: MenuNode[]) {
        const routes: RouteRecordRaw[] = [];
        for (const menu of flattenMenus(tree)) {
            if (
                menu.menuType !== "MENU" ||
                !menu.path ||
                !menu.component ||
                menu.path === "/dashboard"
            ) {
                continue;
            }
            const component = resolveView(menu.component);
            if (!component) {
                continue;
            }
            const meta = {
                title: menu.menuName,
                permission: menu.permission,
                ...(menu.component === "apps/detail/MicroFrontendHost"
                    ? {
                          appCode: menu.menuCode.replace(/^app:/, ""),
                          basePath: menu.path,
                      }
                    : {}),
            };
            routes.push({
                path: menu.path,
                name: menu.menuCode,
                component,
                meta,
            });
            if (menu.component === "apps/detail/MicroFrontendHost") {
                routes.push({
                    path: `${menu.path}/:pathMatch(.*)*`,
                    name: `${menu.menuCode}:deep`,
                    component,
                    meta,
                });
            }
        }
        return routes;
    }

    async function loadAndBuild(router: Router, force = false) {
        if (loaded.value && !force) {
            return;
        }
        if (loadPromise) {
            return loadPromise;
        }
        const userStore = useUserStore();
        const epoch = userStore.sessionEpoch;
        const generation = snapshotGeneration;
        let pending: Promise<void>;
        pending = (async () => {
            const tree = await getMyMenus();
            if (
                epoch !== userStore.sessionEpoch ||
                generation !== snapshotGeneration ||
                !userStore.isLogin
            ) {
                throw new Error("STALE_SESSION");
            }
            const routes = buildRoutes(tree);
            removers.forEach((remove) => remove());
            removers.length = 0;
            for (const route of routes) {
                removers.push(router.addRoute("Layout", route));
            }
            menus.value = tree;
            loaded.value = true;
        })();
        loadPromise = pending;
        try {
            await pending;
        } finally {
            if (loadPromise === pending) {
                loadPromise = null;
            }
        }
    }

    function reset() {
        snapshotGeneration += 1;
        loadPromise = null;
        removers.forEach((remove) => remove());
        removers.length = 0;
        menus.value = [];
        loaded.value = false;
    }

    return { menus, loaded, homePath, loadAndBuild, reset };
});
