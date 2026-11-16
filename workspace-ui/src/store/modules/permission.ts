import { computed, ref } from "vue";
import { defineStore } from "pinia";
import type { Component } from "vue";
import type { Router } from "vue-router";
import { getMyMenus } from "@/api/system/menu";
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

    async function loadAndBuild(router: Router) {
        if (loaded.value) {
            return;
        }
        const tree = await getMyMenus();
        menus.value = tree;
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
            removers.push(
                router.addRoute("Layout", {
                    path: menu.path,
                    name: menu.menuCode,
                    component,
                    meta: {
                        title: menu.menuName,
                        permission: menu.permission,
                    },
                }),
            );
        }
        loaded.value = true;
    }

    function reset() {
        removers.forEach((remove) => remove());
        removers.length = 0;
        menus.value = [];
        loaded.value = false;
    }

    return { menus, loaded, homePath, loadAndBuild, reset };
});
