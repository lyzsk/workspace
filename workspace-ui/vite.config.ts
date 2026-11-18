import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";
export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd());
    const devOrigin = "http://localhost:5173";
    const eapUiTarget = env.VITE_EAP_UI_PROXY || "http://localhost:3006";
    const eapUiOrigin = env.VITE_EAP_UI_ORIGIN || "http://localhost:3006";
    const wsTestUiTarget = env.VITE_WS_TEST_UI_PROXY || "http://localhost:3007";
    const wsTestUiOrigin = env.VITE_WS_TEST_UI_ORIGIN || "http://localhost:3007";

    return {
        plugins: [vue()],
        resolve: {
            alias: {
                "@": fileURLToPath(new URL("./src", import.meta.url)),
            },
        },
        server: {
            host: "0.0.0.0",
            strictPort: true,
            port: 5173,
            proxy: {
                "/eap": {
                    target: eapUiTarget,
                    changeOrigin: true,
                    ws: true,
                    xfwd: true,
                    configure: (proxy) => {
                        proxy.on("proxyReq", (proxyReq) => {
                            proxyReq.setHeader("origin", eapUiOrigin);
                        });
                        proxy.on("proxyReqWs", (proxyReq) => {
                            proxyReq.setHeader("origin", eapUiOrigin);
                        });
                    },
                },
                "/ws-test": {
                    target: wsTestUiTarget,
                    changeOrigin: true,
                    ws: true,
                    xfwd: true,
                    configure: (proxy) => {
                        proxy.on("proxyReq", (proxyReq) => {
                            proxyReq.setHeader("origin", wsTestUiOrigin);
                        });
                        proxy.on("proxyReqWs", (proxyReq) => {
                            proxyReq.setHeader("origin", wsTestUiOrigin);
                        });
                    },
                },
                "/api": {
                    target: env.VITE_API_PROXY || "http://localhost:61114",
                    changeOrigin: true,
                    xfwd: true,
                    configure: (proxy) => {
                        proxy.on("proxyReq", (proxyReq) => {
                            proxyReq.setHeader("origin", devOrigin);
                        });
                    },
                },
                "/ws": {
                    target: env.VITE_API_PROXY || "http://localhost:61114",
                    changeOrigin: true,
                    ws: true,
                    configure: (proxy) => {
                        proxy.on("proxyReqWs", (proxyReq) => {
                            proxyReq.setHeader("origin", devOrigin);
                        });
                    },
                },
                "/doc.html": {
                    target: env.VITE_API_PROXY || "http://localhost:61114",
                    changeOrigin: true,
                },
                "/v3/api-docs": {
                    target: env.VITE_API_PROXY || "http://localhost:61114",
                    changeOrigin: true,
                },
                "/webjars": {
                    target: env.VITE_API_PROXY || "http://localhost:61114",
                    changeOrigin: true,
                },
                "/swagger-ui": {
                    target: env.VITE_API_PROXY || "http://localhost:61114",
                    changeOrigin: true,
                },
                "/swagger-resources": {
                    target: env.VITE_API_PROXY || "http://localhost:61114",
                    changeOrigin: true,
                },
            },
        },
    };
});
