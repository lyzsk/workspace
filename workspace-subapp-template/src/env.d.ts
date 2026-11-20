/// <reference types="vite/client" />

declare module "*.vue" {
    import type { DefineComponent } from "vue";
    const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>;
    export default component;
}

interface ImportMetaEnv {
    readonly VITE_PORT: string;
    readonly VITE_BASE_URL: string;
    readonly VITE_API_BASE_URL: string;
    readonly VITE_API_PROXY: string;
    readonly VITE_USE_MOCK: string;
    readonly VITE_APP_CODE: string;
    readonly VITE_APP_BASE_PATH: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}

interface Window {
    /** qiankun 注入的标记，用于区分「被宿主加载」与「独立运行」 */
    __POWERED_BY_QIANKUN__?: boolean;
}
