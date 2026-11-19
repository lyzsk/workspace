import axios, { type AxiosResponse } from "axios";
import { AppConfig } from "@/config";
import type {
    ApiResult,
    LoginPayload,
    LoginResult,
    UserInfo,
} from "@/types/api";

const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

const MOCK_USER: UserInfo = {
    id: "1",
    username: "admin",
    realName: "管理员",
    gender: 1,
    tenantId: "0",
    tenantName: "默认公司",
};

const client = axios.create({
    baseURL: AppConfig.api.baseURL,
    timeout: AppConfig.api.timeout,
    withCredentials: true,
});

export class AuthApiError extends Error {
    readonly status: number;

    constructor(status: number, message: string) {
        super(message);
        this.name = "AuthApiError";
        this.status = status;
    }
}

function wait(ms: number) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

function unwrap<T>(response: AxiosResponse<ApiResult<T>>): T {
    const body = response.data;
    if (body.code !== 200) {
        throw new AuthApiError(body.code, body.msg || "REQUEST_FAILED");
    }
    return body.data;
}

export function isAuthUnauthorized(error: unknown) {
    return (
        (axios.isAxiosError(error) && error.response?.status === 401) ||
        (error instanceof AuthApiError && error.status === 401)
    );
}

export async function loginRequest(
    payload: LoginPayload,
): Promise<LoginResult> {
    if (USE_MOCK) {
        await wait(500);
        if (payload.username && payload.password) {
            return {
                token: `mock-token-${Date.now()}`,
                user: MOCK_USER,
            };
        }
        throw new Error("INVALID_CREDENTIALS");
    }
    return unwrap(
        await client.post<ApiResult<LoginResult>>("/auth/login", payload),
    );
}

export async function getSessionUser(token: string): Promise<UserInfo> {
    if (USE_MOCK) {
        await wait(200);
        return MOCK_USER;
    }
    return unwrap(
        await client.get<ApiResult<UserInfo>>("/auth/me", {
            headers: { Authorization: `Bearer ${token}` },
        }),
    );
}

export async function refreshRequest(token: string): Promise<LoginResult> {
    if (USE_MOCK) {
        await wait(200);
        return {
            token: `mock-token-${Date.now()}`,
            user: MOCK_USER,
        };
    }
    const refresh = async () => unwrap(
        await client.post<ApiResult<LoginResult>>("/auth/refresh", undefined, {
            headers: { Authorization: `Bearer ${token}` },
        }),
    );
    if (typeof navigator !== "undefined" && navigator.locks) {
        return navigator.locks.request("workspace-auth-refresh", { mode: "exclusive" }, refresh);
    }
    return refresh();
}

export async function logoutRequest(token: string): Promise<void> {
    if (USE_MOCK) {
        return;
    }
    await client.post<ApiResult<void>>(
        "/auth/logout",
        undefined,
        token
            ? { headers: { Authorization: `Bearer ${token}` } }
            : undefined,
    );
}
