import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from "axios";
import { ElLoading, ElMessage } from "element-plus";
import { AppConfig } from "@/config";
import { useUserStore } from "@/store/modules/user";
import type { ApiResult } from "@/types/api";
import { handleSessionExpired, markExpired } from "@/utils/session";

interface SilentConfig extends InternalAxiosRequestConfig {
    _silent?: boolean;
}

interface RequestOptions {
    silent?: boolean;
}

const service = axios.create({
    baseURL: AppConfig.api.baseURL,
    timeout: AppConfig.api.timeout,
    withCredentials: AppConfig.api.withCredentials,
});

service.interceptors.request.use((config) => {
    const token = useUserStore().token;
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

service.interceptors.response.use(
    (response) => response,
    (error) => {
        const config = error.config as SilentConfig | undefined;
        if (error.response?.status === 401) {
            if (config?._silent) {
                markExpired();
            } else {
                handleSessionExpired("expired");
            }
        } else if (error.response?.status === 403 && !config?._silent) {
            const body = error.response.data as ApiResult<unknown> | undefined;
            ElMessage.error(body?.msg || "无权限");
        }
        return Promise.reject(error);
    },
);

export async function download(config: AxiosRequestConfig, filename: string) {
    const loading = ElLoading.service({
        text: "正在导出数据，请稍候",
        background: "rgba(0, 0, 0, 0.7)",
    });
    try {
        const response = await service.request<Blob>({
            ...config,
            responseType: "blob",
        });
        const contentType =
            (response.headers["content-type"] as string | undefined) ?? "";
        if (contentType.includes("application/json")) {
            const text = await (response.data as Blob).text();
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
    const response = await service.request<ApiResult<T>>({
        ...config,
        _silent: options?.silent,
    } as AxiosRequestConfig);
    const body = response.data;

    if (body.code !== 200) {
        if (body.code === 401) {
            if (options?.silent) {
                markExpired();
            } else {
                handleSessionExpired("expired");
            }
        } else if (!options?.silent) {
            ElMessage.error(body.msg || (body.code === 403 ? "无权限" : "请求失败"));
        }
        throw new Error(body.msg || "REQUEST_FAILED");
    }

    return body.data;
}
