export interface ApiResult<T = unknown> {
    code: string;
    msg: string;
    data: T;
}

export interface UserInfo {
    id: string;
    username: string;
    realName: string;
    gender: number;
    email?: string;
    mobile?: string;
    avatar?: string;
    tenantId?: string;
    tenantName?: string;
    deptId?: string;
    deptName?: string;
    roles?: string[];
    permissions?: string[];
}

export interface LoginPayload {
    username: string;
    password: string;
}

export interface ProfilePayload {
    realName: string;
    gender: number;
    email: string;
    mobile: string;
    avatar?: string;
}

export interface PasswordPayload {
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;
}

export interface LoginResult {
    token: string;
    user: UserInfo;
}
