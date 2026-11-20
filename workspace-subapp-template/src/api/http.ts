import axios from "axios";
import { ElMessage } from "element-plus";
import { AppConfig } from "@/config";
import { getToken, reportAuthFailed, reportSessionExpired } from "@/micro/auth";
import type { ApiResult } from "@/types/api";

/**
 * 请求封装。
 *
 * 与主应用的 utils/http 保持同一套写法，方便组内切换项目时不用重新适应。
 *
 * 三个和主应用不同的地方，都是微前端场景特有的：
 * 1. token 每次现取，不缓存在 store 里
 * 2. 401 只上报，不跳转，跳转由主应用统一处理
 * 3. 错误提示在微前端模式下会被主应用捕获展示，这里不再重复弹
 */

const service = axios.create({
    baseURL: AppConfig.api.baseURL,
    timeout: AppConfig.api.timeout,
    withCredentials: AppConfig.api.withCredentials,
});

service.interceptors.request.use((config) => {
    const token = getToken();
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

service.interceptors.response.use(
    (response) => response,
    (error) => {
        const status = error.response?.status;
        const body = error.response?.data as ApiResult<unknown> | undefined;

        if (status === 401) {
            // 会话过期只上报，不在这里跳登录页
            reportSessionExpired();
            return Promise.reject(error);
        }

        if (status === 403) {
            reportAuthFailed(body?.msg || "无权限访问该资源");
            ElMessage.error(body?.msg || "无权限访问该资源");
            return Promise.reject(error);
        }

        // 业务层面的错误，后端通常在 200 响应里带非成功状态码
        const message = body?.msg || error.message || "请求失败";
        ElMessage.error(message);
        return Promise.reject(error);
    },
);

/** 发起 GET 请求并解包 data */
export async function get<T>(url: string, params?: unknown): Promise<T> {
    const response = await service.get<ApiResult<T>>(url, { params });
    return unwrap(response.data);
}

/** 发起 POST 请求并解包 data */
export async function post<T>(url: string, data?: unknown): Promise<T> {
    const response = await service.post<ApiResult<T>>(url, data);
    return unwrap(response.data);
}

/**
 * 解包统一返回结构。
 *
 * 后端可能返回 code=200 表示成功，也可能直接返回裸数据。
 * 这里两种都兼容，避免每个接口都写一遍判断。
 */
function unwrap<T>(body: ApiResult<T>): T {
    if (body !== null && typeof body === "object" && "code" in body) {
        if (body.code === 200) {
            return body.data;
        }
        throw new Error(body.msg || "请求失败");
    }
    return body as unknown as T;
}
