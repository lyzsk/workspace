export interface PageResult<T> {
    records: T[];
    total: string | number;
    pageNum: string | number;
    pageSize: string | number;
}

export interface MenuNode {
    id: string;
    parentId: string;
    menuCode: string;
    menuName: string;
    menuType: "DIR" | "MENU" | "BUTTON";
    path?: string;
    component?: string;
    permission?: string;
    icon?: string;
    sortNo: number;
    hidden: number;
    status: number;
    children?: MenuNode[];
}

export interface RoleRow {
    id: string;
    roleCode: string;
    roleName: string;
    dataScope: number;
    roleSort: number;
    menuCheckStrictly: boolean;
    status: number;
    remark?: string;
    createTime?: string;
    deptIds?: string[];
}

export interface DeptNode {
    id: string;
    parentId: string;
    deptCode: string;
    deptName: string;
    sortNo: number;
    status: number;
    createTime?: string;
    children?: DeptNode[];
}

export interface DictTypeRow {
    id: string;
    dictType: string;
    dictName: string;
    status: number;
    createTime?: string;
    remark?: string;
}

export interface DictDataRow {
    id: string;
    dictType: string;
    label: string;
    value: string;
    sortNo: number;
    status: number;
    createTime?: string;
    remark?: string;
}

export interface LoginLogRow {
    id: string;
    username: string;
    status: number;
    message: string;
    ip: string;
    userAgent: string;
    loginTime: string;
}

export interface OnlineUserRow {
    id: string;
    userId: string;
    username: string;
    realName: string;
    ip: string;
    userAgent: string;
    loginTime: string;
    lastSeenAt: string;
    expiresAt: string;
}

export interface OperLogRow {
    id: string;
    username: string;
    module: string;
    operation: string;
    requestMethod: string;
    requestUri: string;
    requestParams: string;
    responseResult: string;
    status: number;
    errorMsg: string;
    costTime: number;
    ip: string;
    operTime: string;
}
