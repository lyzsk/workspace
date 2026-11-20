/**
 * 应用配置。
 *
 * 所有「换个环境就要改」的值都集中在这里，业务代码不直接读 import.meta.env。
 * 好处是排查问题时只需要看这一个文件，不用全局搜索环境变量。
 */
export const AppConfig = {
    systemInfo: {
        name: "子应用模板",
        version: "1.0.0",
    },
    api: {
        // 开发环境为 /api（走 Vite 代理），生产环境为网关前缀
        baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
        timeout: 15000,
        // 同域部署时携带 Cookie，供后端会话类接口使用
        withCredentials: true,
    },
    micro: {
        // 应用编码。必须与主应用 sys_app.code 一致，否则事件会被宿主过滤掉
        appCode: import.meta.env.VITE_APP_CODE || "subapp",
        // 应用路由前缀。必须与主应用 sys_app.base_path 一致
        basePath: import.meta.env.VITE_APP_BASE_PATH || "/apps/subapp",
        // 独立运行时的占位 token，仅用于让页面能打开，不具备服务端效力
        mockToken: "local-dev-token",
        // 是否使用本地模拟数据，后端未就绪时置 true
        useMock: import.meta.env.VITE_USE_MOCK === "true",
    },
} as const;
