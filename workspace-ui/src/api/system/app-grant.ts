import { request } from "@/utils/http";
import type { MenuNode } from "@/types/system";

export interface UserAppGrant {
    appCode: string;
    appName: string;
    appStatus: number;
    granted: boolean;
    effective: boolean;
    status: number;
    roleIds: string[];
    roleNames: string[];
    dataScope?: number | null;
    deptIds: string[];
}

export interface UserAppGrantForm {
    status: number;
    roleIds: string[];
    dataScope?: number | null;
    deptIds: string[];
}

export interface AppAuthorization {
    appCode: string;
    accessible: boolean;
    roles: string[];
    permissions: string[];
    menus: MenuNode[];
    dataScope?: number | null;
    all: boolean;
    deptIds: string[];
    selfOnly: boolean;
    userId: string;
}

export function listUserAppGrants(userId: string) {
    return request<UserAppGrant[]>({
        url: `/system/users/${userId}/app-grants`,
        method: "get",
    });
}

export function saveUserAppGrant(userId: string, appCode: string, data: UserAppGrantForm) {
    return request<void>({
        url: `/system/users/${userId}/apps/${appCode}/grant`,
        method: "put",
        data,
    });
}

export function getMyAppAuthorization(appCode: string) {
    return request<AppAuthorization>({
        url: `/auth/me/apps/${appCode}/authorization`,
        method: "get",
    });
}
