import { request } from "@/utils/http";
import type { MenuNode } from "@/types/system";

export interface MenuForm {
    appCode?: string;
    menuCode: string;
    parentId?: string;
    menuName: string;
    menuType: string;
    path?: string;
    component?: string;
    permission?: string;
    icon?: string;
    sortNo?: number;
    hidden?: number;
    status?: number;
}

export function getMyMenus() {
    return request<MenuNode[]>({ url: "/system/menus/my", method: "get" });
}

export function getMenuTree(includeButtons = false) {
    return request<MenuNode[]>({
        url: "/system/menus/tree",
        method: "get",
        params: { includeButtons },
    });
}

export function createMenu(data: MenuForm) {
    return request<string>({ url: "/system/menus", method: "post", data });
}

export function updateMenu(id: string, data: MenuForm) {
    return request<void>({
        url: `/system/menus/${id}`,
        method: "put",
        data,
    });
}

export function deleteMenu(id: string) {
    return request<void>({ url: `/system/menus/${id}`, method: "delete" });
}
