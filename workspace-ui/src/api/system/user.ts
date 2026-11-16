import { download, request } from "@/utils/http";
import { formatFileStamp } from "@/utils/format";
import type { PageResult } from "@/types/system";

export interface UserRow {
    id: string;
    username: string;
    realName: string;
    gender: number;
    email?: string;
    mobile?: string;
    deptId?: string;
    status: number;
    createTime?: string;
    roleIds?: string[];
}

export interface UserForm {
    username: string;
    password?: string;
    realName: string;
    gender?: number;
    email?: string;
    mobile?: string;
    deptId?: string;
    status?: number;
    roleIds?: string[];
}

export function pageUsers(params: Record<string, unknown>) {
    return request<PageResult<UserRow>>({
        url: "/system/users",
        method: "get",
        params,
    });
}

export function createUser(data: UserForm) {
    return request<string>({ url: "/system/users", method: "post", data });
}

export function updateUser(id: string, data: UserForm) {
    return request<void>({
        url: `/system/users/${id}`,
        method: "put",
        data,
    });
}

export function deleteUser(id: string) {
    return request<void>({ url: `/system/users/${id}`, method: "delete" });
}

export function deleteUsers(ids: string[]) {
    return request<void>({
        url: "/system/users/batch",
        method: "delete",
        data: ids,
    });
}

export function exportUsers(params: Record<string, unknown>) {
    return download(
        { url: "/system/users/export", method: "get", params },
        `users_${formatFileStamp()}.xlsx`,
    );
}
