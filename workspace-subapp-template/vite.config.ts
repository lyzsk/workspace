import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd());
    const isProduction = mode === "production";
    const isMicroBuild = mode === "micro";

    return {
        // 生产环境部署在 /ies/ 子路径下，必须与主应用的入口地址保持一致
        base: env.VITE_BASE_URL || "/",
        plugins: [vue()],
        resolve: {
            alias: {
                "@": fileURLToPath(new URL("./src", import.meta.url)),
            },
        },
        server: {
            host: "0.0.0.0",
            // 端口必须固定：主应用按固定地址加载子应用，端口漂移会导致入口不可达
            port: Number(env.VITE_PORT) || 3010,
            strictPort: true,
            // 主应用跨域加载子应用 HTML 与资源，开发服务需要放开跨域
            cors: true,
            headers: {
                "Access-Control-Allow-Origin": "*",
            },
            proxy: {
                "/api": {
                    target: env.VITE_API_PROXY,
                    changeOrigin: true,
                },
            },
        },
        preview: {
            host: "0.0.0.0",
            port: Number(env.VITE_PORT) || 3010,
            strictPort: true,
        },
        build: {
            target: "es2015",
            outDir: isMicroBuild ? "dist/micro" : "dist",
            sourcemap: !isProduction,
            chunkSizeWarningLimit: 1500,
            lib: isMicroBuild
                ? {
                      entry: fileURLToPath(new URL("./src/main.ts", import.meta.url)),
                      formats: ["es"],
                      fileName: "subapp-micro",
                      cssFileName: "subapp-micro",
                  }
                : undefined,
            rollupOptions: {
                preserveEntrySignatures: "strict",
            },
        },
    };
});
