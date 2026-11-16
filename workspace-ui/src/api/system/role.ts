import { download, request } from "@/utils/http";
import { formatFileStamp } from "@/utils/format";
import type { PageResult, RoleRow } from "@/types/system";

export interface RoleForm {
    roleCode: string;
    roleName: string;
    dataScope?: number;
    roleSort?: number;
    menuCheckStrictly?: boolean;
    status?: number;
    remark?: string;
    menuIds?: string[];
    deptIds?: string[];
}

export function pageRoles(params: Record<string, unknown>) {
    return request<PageResult<RoleRow>>({
        url: "/system/roles",
        method: "get",
        params,
    });
}

export function listRoles() {
    return request<RoleRow[]>({ url: "/system/roles/list", method: "get" });
}

export function createRole(data: RoleForm) {
    return request<string>({ url: "/system/roles", method: "post", data });
}

export function updateRole(id: string, data: RoleForm) {
    return request<void>({
        url: `/system/roles/${id}`,
        method: "put",
        data,
    });
}

export function deleteRole(id: string) {
    return request<void>({ url: `/system/roles/${id}`, method: "delete" });
}

export function exportRoles(params: Record<string, unknown>) {
    return download(
        { url: "/system/roles/export", method: "get", params },
        `roles_${formatFileStamp()}.xlsx`,
    );
}

export function getRoleMenuIds(id: string) {
    return request<string[]>({
        url: `/system/roles/${id}/menus`,
        method: "get",
    });
}

export function assignRoleMenus(id: string, menuIds: string[]) {
    return request<void>({
        url: `/system/roles/${id}/menus`,
        method: "put",
        data: { menuIds },
    });
}
