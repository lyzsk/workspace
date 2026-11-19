import axios, {
    type AxiosRequestConfig,
    type AxiosResponse,
    type InternalAxiosRequestConfig,
} from "axios";
import { ElLoading, ElMessage } from "element-plus";
import { AppConfig } from "@/config";
import { useUserStore } from "@/store/modules/user";
import type { ApiResult } from "@/types/api";

interface AuthRequestConfig extends AxiosRequestConfig {
    _silent?: boolean;
    _skipAuthRefresh?: boolean;
    _authRetried?: boolean;
    _sessionEpoch?: number;
}

interface RequestOptions {
    silent?: boolean;
}

export class StaleRequestError extends Error {
    constructor() {
        super("STALE_SESSION");
        this.name = "StaleRequestError";
    }
}

const service = axios.create({
    baseURL: AppConfig.api.baseURL,
    timeout: AppConfig.api.timeout,
    withCredentials: AppConfig.api.withCredentials,
});

service.interceptors.request.use((config) => {
    const userStore = useUserStore();
    const authConfig = config as InternalAxiosRequestConfig & AuthRequestConfig;
    const token = userStore.token;
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
        authConfig._sessionEpoch ??= userStore.sessionEpoch;
    }
    return config;
});

function isStale(config: AuthRequestConfig | undefined) {
    return (
        config?._sessionEpoch !== undefined &&
        config._sessionEpoch !== useUserStore().sessionEpoch
    );
}

function apiCode(data: unknown) {
    if (
        typeof data === "object" &&
        data !== null &&
        "code" in data &&
        typeof data.code === "number"
    ) {
        return data.code;
    }
    return undefined;
}

async function responseCode(response: AxiosResponse) {
    const plainCode = apiCode(response.data);
    if (plainCode !== undefined) {
        return plainCode;
    }
    const contentType =
        (response.headers["content-type"] as string | undefined) ?? "";
    if (
        response.data instanceof Blob &&
        contentType.includes("application/json")
    ) {
        const body = JSON.parse(await response.data.text()) as ApiResult<unknown>;
        return body.code;
    }
    return undefined;
}

async function retryUnauthorized(
    config: AuthRequestConfig,
    cause: unknown,
): Promise<AxiosResponse> {
    const userStore = useUserStore();
    if (
        config._skipAuthRefresh ||
        !userStore.isLogin ||
        isStale(config)
    ) {
        throw cause;
    }
    if (config._authRetried) {
        await userStore.expire("expired");
        throw cause;
    }
    try {
        await userStore.refresh();
    } catch {
        throw cause;
    }
    if (isStale(config)) {
        throw new StaleRequestError();
    }
    return send({
        ...config,
        _authRetried: true,
    });
}

async function send<T>(config: AuthRequestConfig): Promise<AxiosResponse<T>> {
    let response: AxiosResponse<T>;
    try {
        response = await service.request<T>(config);
    } catch (error) {
        const requestConfig = axios.isAxiosError(error)
            ? (error.config as AuthRequestConfig | undefined)
            : undefined;
        if (isStale(requestConfig)) {
            throw new StaleRequestError();
        }
        if (
            axios.isAxiosError(error) &&
            error.response?.status === 401 &&
            requestConfig
        ) {
            return (await retryUnauthorized(requestConfig, error)) as AxiosResponse<T>;
        }
        if (axios.isAxiosError(error) && error.response?.status === 403) {
            if (!requestConfig?._silent) {
                const body = error.response.data as ApiResult<unknown> | undefined;
                ElMessage.error(body?.msg || "无权限");
            }
        }
        throw error;
    }
    if (isStale(response.config)) {
        throw new StaleRequestError();
    }
    if ((await responseCode(response)) === 401) {
        return (await retryUnauthorized(
            response.config,
            new Error("REQUEST_UNAUTHORIZED"),
        )) as AxiosResponse<T>;
    }
    return response;
}

export async function download(config: AxiosRequestConfig, filename: string) {
    const loading = ElLoading.service({
        text: "正在导出数据，请稍候",
        background: "rgba(0, 0, 0, 0.7)",
    });
    try {
        const response = await send<Blob>({
            ...config,
            responseType: "blob",
        });
        const contentType =
            (response.headers["content-type"] as string | undefined) ?? "";
        if (contentType.includes("application/json")) {
            const text = await response.data.text();
            const body = JSON.parse(text) as ApiResult<unknown>;
            ElMessage.error(body.msg || "导出失败");
            throw new Error(body.msg || "EXPORT_FAILED");
        }
        const blob = new Blob([response.data], {
            type: contentType || "application/octet-stream",
        });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    } finally {
        loading.close();
    }
}

export async function request<T>(
    config: AxiosRequestConfig,
    options?: RequestOptions,
): Promise<T> {
    const response = await send<ApiResult<T>>({
        ...config,
        _silent: options?.silent,
    });
    const body = response.data;

    if (body.code !== 200) {
        if (!options?.silent) {
            ElMessage.error(body.msg || (body.code === 403 ? "无权限" : "请求失败"));
        }
        throw new Error(body.msg || "REQUEST_FAILED");
    }

    return body.data;
}
