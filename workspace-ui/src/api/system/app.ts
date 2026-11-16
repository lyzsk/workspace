import { request } from "@/utils/http";

export interface AppInfo {
    id: string;
    code: string;
    name: string;
    icon?: string;
    basePath: string;
    moduleEntry: string;
    styleEntry?: string;
    accessPermission: string;
    sortNo: number;
    status: number;
    remark?: string;
}

export interface AppForm {
    code: string;
    name: string;
    icon: string;
    basePath: string;
    moduleEntry: string;
    styleEntry: string;
    accessPermission: string;
    sortNo: number;
    status: number;
    remark: string;
}

export function listApps() {
    return request<AppInfo[]>({ url: "/system/apps", method: "get" });
}

export function getAppRuntime(path: string) {
    return request<AppInfo>({
        url: "/system/apps/runtime",
        method: "get",
        params: { path },
    });
}

export function createApp(data: AppForm) {
    return request<string>({ url: "/system/apps", method: "post", data });
}

export function updateApp(id: string, data: AppForm) {
    return request<void>({ url: `/system/apps/${id}`, method: "put", data });
}

export function deleteApp(id: string) {
    return request<void>({ url: `/system/apps/${id}`, method: "delete" });
}
