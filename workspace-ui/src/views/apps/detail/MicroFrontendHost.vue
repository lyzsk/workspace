<template>
    <div class="micro-host">
        <div
            v-show="app && !error"
            ref="microContainer"
            class="micro-container"
        ></div>

        <div v-if="showSkeleton && !error" class="micro-skeleton">
            <el-skeleton :rows="6" animated />
        </div>

        <div v-if="error" class="app-error">
            <el-empty :description="error" />
            <el-button type="primary" @click="load">重试</el-button>
        </div>

        <el-empty
            v-else-if="!app && !loading"
            description="应用不存在或未启用"
        />
    </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
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
import {
    resolveMicroHostPath,
    resolveMicroInitialPath,
    resolveMicroInternalPath,
} from "@/utils/microFrontendPath";
import { mountMicroApplication } from "@/utils/qiankun";

defineOptions({ name: "MicroFrontendHost" });

const route = useRoute();
const router = useRouter();
const appStore = useAppStore();
const userStore = useUserStore();
const app = ref<AppInfo | null>(null);
const loading = ref(false);
const showSkeleton = ref(false);
const error = ref("");
const microContainer = ref<HTMLElement>();

let disposed = false;
let routeSequence = 0;
let readySequence = -1;
let activeCode = "";
let activePath = "";
let cleanupMicro: (() => Promise<void>) | null = null;
let offMicroEvent: (() => void) | null = null;
let loadingTimer: number | undefined;
let readyWatchdog: number | undefined;

function clearLoadingTimer() {
    if (loadingTimer !== undefined) {
        window.clearTimeout(loadingTimer);
        loadingTimer = undefined;
    }
}

function clearReadyWatchdog() {
    if (readyWatchdog !== undefined) {
        window.clearTimeout(readyWatchdog);
        readyWatchdog = undefined;
    }
}

function startLoading() {
    loading.value = true;
    clearLoadingTimer();
    loadingTimer = window.setTimeout(() => {
        if (!disposed && loading.value) {
            showSkeleton.value = true;
        }
        loadingTimer = undefined;
    }, 300);
}

function stopLoading(sequence: number) {
    if (sequence !== routeSequence) {
        return;
    }
    clearLoadingTimer();
    loading.value = false;
    showSkeleton.value = false;
}

function currentInternalPath(basePath = activePath) {
    return resolveMicroInitialPath(route.fullPath, basePath);
}

function onMicroEvent(detail: MicroFrontendEventDetail) {
    if (
        detail.version !== 1 ||
        detail.appCode !== app.value?.code ||
        detail.appCode !== route.meta.appCode
    ) {
        return;
    }
    if (detail.type === "app.ready") {
        readySequence = routeSequence;
        clearReadyWatchdog();
        error.value = "";
        return;
    }
    if (detail.type === "app.route.changed") {
        const basePath = app.value.basePath;
        const internalPath = resolveMicroInternalPath(detail.path, basePath);
        const titles = detail.titles?.length
            ? detail.titles
            : detail.title
              ? [detail.title]
              : [];
        appStore.setActiveAppPage(titles, internalPath);
        const target = resolveMicroHostPath(basePath, internalPath);
        if (route.fullPath !== target) {
            void router.replace(target).catch(() => undefined);
        }
        return;
    }
    if (detail.type === "app.auth.failed") {
        ElMessage.error(detail.message || "无权限访问该资源");
        return;
    }
    if (detail.type === "app.error") {
        clearReadyWatchdog();
        error.value = detail.message || "子应用运行异常";
        showSkeleton.value = false;
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
    if (detail.type === "app.session.expired") {
        void userStore.refresh().catch(() => undefined);
    }
}

function isCurrent(sequence: number, appCode: string, basePath: string) {
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
    clearReadyWatchdog();
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
    readySequence = -1;
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
            initialPath: currentInternalPath(current.basePath),
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
        emitMicroFrontendEvent("host.route.changed", {
            appCode: current.code,
            path: currentInternalPath(current.basePath),
        });
        if (readySequence !== sequence && !error.value) {
            clearReadyWatchdog();
            readyWatchdog = window.setTimeout(() => {
                if (
                    isCurrent(sequence, current.code, current.basePath) &&
                    activeCode === current.code
                ) {
                    error.value = "子应用渲染超时，请重试";
                    showSkeleton.value = false;
                    emitMicroFrontendEvent("host.app.error", {
                        appCode: current.code,
                        path: current.basePath,
                        message: error.value,
                    });
                }
            }, 10000);
        }
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
    }
}

async function load() {
    const sequence = ++routeSequence;
    const appCode = route.meta.appCode;
    const basePath = route.meta.basePath;
    startLoading();
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
        stopLoading(sequence);
    }
}

watch(
    [() => route.meta.appCode, () => route.meta.basePath],
    () => {
        if (!disposed) {
            void load();
        }
    },
);

watch(
    () => route.fullPath,
    () => {
        if (
            disposed ||
            !activeCode ||
            route.meta.appCode !== activeCode ||
            route.meta.basePath !== activePath
        ) {
            return;
        }
        emitMicroFrontendEvent("host.route.changed", {
            appCode: activeCode,
            path: currentInternalPath(),
        });
    },
);

onMounted(() => {
    offMicroEvent = onMicroFrontendEvent(onMicroEvent);
    void load();
});
onUnmounted(() => {
    disposed = true;
    routeSequence += 1;
    clearLoadingTimer();
    clearReadyWatchdog();
    loading.value = false;
    showSkeleton.value = false;
    appStore.clearActiveApp();
    offMicroEvent?.();
    offMicroEvent = null;
    void cleanupCurrentMicro().catch(() => undefined);
});
</script>

<style scoped>
.micro-host {
    position: relative;
    height: 100%;
    min-height: 0;
}

.micro-skeleton {
    position: absolute;
    inset: 0;
    z-index: 1;
    padding: 16px;
    background: var(--default-bg-color, var(--app-bg, #fafbfc));
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

.micro-container :deep(.layout-sidebar),
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
