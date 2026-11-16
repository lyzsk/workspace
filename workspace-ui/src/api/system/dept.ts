import { request } from "@/utils/http";
import type { DeptNode } from "@/types/system";

export interface DeptForm {
    deptCode: string;
    deptName: string;
    parentId?: string;
    sortNo?: number;
    status?: number;
}

export function getDeptTree() {
    return request<DeptNode[]>({ url: "/system/depts/tree", method: "get" });
}

export function getDeptList() {
    return request<DeptNode[]>({ url: "/system/depts/list", method: "get" });
}

export function createDept(data: DeptForm) {
    return request<string>({ url: "/system/depts", method: "post", data });
}

export function updateDept(id: string, data: DeptForm) {
    return request<void>({
        url: `/system/depts/${id}`,
        method: "put",
        data,
    });
}

export function deleteDept(id: string) {
    return request<void>({ url: `/system/depts/${id}`, method: "delete" });
}
