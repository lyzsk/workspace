<template>
    <div v-loading="loading" class="micro-host">
        <div
            v-show="app && !error"
            ref="microContainer"
            class="micro-container"
        ></div>

        <div v-if="error" class="app-error">
            <el-empty :description="error" />
            <el-button type="primary" @click="load">重试</el-button>
        </div>

        <el-empty v-else-if="!app" description="应用不存在或未启用" />
    </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { getAppRuntime, type AppInfo } from "@/api/system/app";
import { recordApplicationAccess } from "@/api/system/dashboard";
import { authBridge, createScopedAuthBridge } from "@/microfrontend/authBridge";
import { useAppStore } from "@/store/modules/app";
import { useUserStore } from "@/store/modules/user";
import {
    emitMicroFrontendEvent,
    onMicroFrontendEvent,
    type MicroFrontendEventDetail,
} from "@/utils/microFrontendEvents";
import { mountMicroApplication } from "@/utils/qiankun";

defineOptions({ name: "MicroFrontendHost" });

const route = useRoute();
const appStore = useAppStore();
const userStore = useUserStore();
const app = ref<AppInfo | null>(null);
const loading = ref(false);
const error = ref("");
const microContainer = ref<HTMLElement>();

let disposed = false;
let routeSequence = 0;
let activeCode = "";
let activePath = "";
let cleanupMicro: (() => Promise<void>) | null = null;
let offMicroEvent: (() => void) | null = null;

function onMicroEvent(detail: MicroFrontendEventDetail) {
    if (detail.version !== 1 || detail.appCode !== app.value?.code) {
        return;
    }
    if (detail.type === "app.route.changed") {
        const titles = detail.titles?.length
            ? detail.titles
            : detail.title
              ? [detail.title]
              : [];
        appStore.setActiveAppPage(titles, detail.path);
        return;
    }
    if (detail.type === "app.theme.changed" && detail.theme) {
        if (appStore.isDark !== (detail.theme === "dark")) {
            appStore.isDark = detail.theme === "dark";
            appStore.applyTheme(true);
        }
        return;
    }
    if (detail.type === "app.logout") {
        void userStore.logout("microfrontend");
        return;
    }
    if (
        detail.type === "app.session.expired" ||
        detail.type === "app.auth.failed"
    ) {
        void userStore.refresh().catch(() => undefined);
    }
}

function isCurrent(
    sequence: number,
    appCode: string,
    basePath: string,
) {
    return (
        !disposed &&
        sequence === routeSequence &&
        route.meta.appCode === appCode &&
        route.meta.basePath === basePath
    );
}

function assertCurrent(sequence: number, appCode: string, basePath: string) {
    if (!isCurrent(sequence, appCode, basePath)) {
        throw new Error("STALE_MICRO_FRONTEND_LOAD");
    }
}

async function cleanupCurrentMicro() {
    const cleanup = cleanupMicro;
    const code = activeCode;
    const path = activePath;
    cleanupMicro = null;
    activeCode = "";
    activePath = "";
    if (!cleanup) {
        return;
    }
    try {
        await cleanup();
    } finally {
        if (code) {
            emitMicroFrontendEvent("host.app.left", {
                appCode: code,
                path,
            });
        }
    }
}

async function enter(current: AppInfo, sequence: number) {
    error.value = "";
    loading.value = true;
    try {
        await cleanupCurrentMicro();
        await nextTick();
        assertCurrent(sequence, current.code, current.basePath);
        if (!microContainer.value) {
            throw new Error("微前端挂载容器未就绪");
        }
        const cleanup = await mountMicroApplication({
            name: `workspace:${current.code}`,
            path: current.basePath,
            entry: current.moduleEntry,
            styleEntry: current.styleEntry,
            appCode: current.code,
            theme: appStore.isDark ? "dark" : "light",
            locale: appStore.locale,
            user: authBridge.getUser(),
            authBridge: createScopedAuthBridge(current.code),
            domElement: microContainer.value,
        });
        if (!isCurrent(sequence, current.code, current.basePath)) {
            await cleanup();
            return;
        }
        cleanupMicro = cleanup;
        activeCode = current.code;
        activePath = current.basePath;
        emitMicroFrontendEvent("host.app.entered", {
            appCode: current.code,
            path: current.basePath,
        });
        void recordApplicationAccess(current.code).catch(() => undefined);
    } catch (cause) {
        if (isCurrent(sequence, current.code, current.basePath)) {
            error.value =
                cause instanceof Error
                    ? cause.message
                    : "进入应用失败，请稍后重试";
            emitMicroFrontendEvent("host.app.error", {
                appCode: current.code,
                path: current.basePath,
                message: error.value,
            });
        }
    } finally {
        if (sequence === routeSequence) {
            loading.value = false;
        }
    }
}

async function load() {
    const sequence = ++routeSequence;
    const appCode = route.meta.appCode;
    const basePath = route.meta.basePath;
    loading.value = true;
    error.value = "";
    try {
        await cleanupCurrentMicro();
        if (typeof appCode !== "string" || !appCode) {
            throw new Error("应用路由缺少 appCode");
        }
        if (typeof basePath !== "string" || !basePath) {
            throw new Error("应用路由缺少 basePath");
        }
        const current = await getAppRuntime(basePath);
        assertCurrent(sequence, appCode, basePath);
        if (current.code !== appCode || current.basePath !== basePath) {
            throw new Error("应用 runtime 与菜单路由配置不一致");
        }
        app.value = current;
        appStore.setActiveApp(current.name);
        await nextTick();
        await enter(current, sequence);
    } catch (cause) {
        if (
            sequence === routeSequence &&
            !disposed &&
            !(cause instanceof Error && cause.message === "STALE_SESSION")
        ) {
            app.value = null;
            error.value =
                cause instanceof Error ? cause.message : "应用配置加载失败";
        }
    } finally {
        if (sequence === routeSequence) {
            loading.value = false;
        }
    }
}

watch(
    () => [route.meta.appCode, route.meta.basePath],
    () => {
        if (!disposed) {
            void load();
        }
    },
);

onMounted(() => {
    offMicroEvent = onMicroFrontendEvent(onMicroEvent);
    void load();
});
onUnmounted(() => {
    disposed = true;
    routeSequence += 1;
    appStore.clearActiveApp();
    offMicroEvent?.();
    offMicroEvent = null;
    void cleanupCurrentMicro().catch(() => undefined);
});
</script>

<style scoped>
.micro-host {
    height: 100%;
    min-height: 0;
}

.micro-container {
    height: 100%;
    min-height: 0;
    background: var(--default-bg-color, var(--app-bg, #fafbfc));
    color: var(--art-gray-900, var(--text, #383853));
}

.micro-container :deep(.app-layout) {
    height: 100%;
    min-height: 0;
}

.micro-container :deep(.layout-sidebar),
.micro-container :deep(.layout-sidebar .menu-left),
.micro-container :deep(#app-main) {
    height: 100%;
}

.micro-container :deep(#app-header),
.micro-container :deep(.layout-sidebar .menu-left > .header) {
    display: none;
}

.app-error {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    gap: 12px;
}
</style>
