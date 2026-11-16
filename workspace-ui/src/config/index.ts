export const AppConfig = {
    systemInfo: {
        name: "Workspace",
        version: "1.0.0",
    },
    api: {
        baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
        timeout: 15000,
        withCredentials: true,
    },
} as const;
