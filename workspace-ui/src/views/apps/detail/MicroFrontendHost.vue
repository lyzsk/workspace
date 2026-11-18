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
import { useRoute, useRouter } from "vue-router";
import { getAppRuntime, type AppInfo } from "@/api/system/app";
import { logout as logoutApi } from "@/api/auth";
import { useAppStore } from "@/store/modules/app";
import { usePermissionStore } from "@/store/modules/permission";
import { useUserStore } from "@/store/modules/user";
import { clearDraftsForUser } from "@/utils/draft";
import {
    emitMicroFrontendEvent,
    onMicroFrontendEvent,
    type MicroFrontendEventDetail,
} from "@/utils/microFrontendEvents";
import { handleSessionExpired } from "@/utils/session";
import { registerMicroApplication } from "@/utils/singleSpa";

defineOptions({ name: "MicroFrontendHost" });

const route = useRoute();
const router = useRouter();
const appStore = useAppStore();
const permissionStore = usePermissionStore();
const userStore = useUserStore();
const app = ref<AppInfo | null>(null);
const loading = ref(false);
const error = ref("");
const microContainer = ref<HTMLElement>();

let disposed = false;
let activeCode = "";
let cleanupMicro: (() => Promise<void>) | null = null;
let offMicroEvent: (() => void) | null = null;
let loggingOut = false;

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
        void handleSubappLogout();
        return;
    }
    if (detail.type === "app.session.expired") {
        handleMicroSessionExpired();
    }
}

async function handleSubappLogout() {
    if (loggingOut) {
        return;
    }
    loggingOut = true;
    try {
        await logoutApi({ silent: true });
    } catch {
    }
    permissionStore.reset();
    clearDraftsForUser(userStore.user?.id);
    userStore.logout();
    await router.replace("/login");
}

function handleMicroSessionExpired() {
    if (activeCode) {
        emitMicroFrontendEvent("host.session.expired", {
            appCode: activeCode,
            path: route.path,
        });
    }
    handleSessionExpired("expired");
}

async function cleanupCurrentMicro() {
    const cleanup = cleanupMicro;
    cleanupMicro = null;
    if (cleanup) {
        await cleanup();
        if (activeCode) {
            emitMicroFrontendEvent("host.app.left", {
                appCode: activeCode,
                path: route.path,
            });
        }
    }
    activeCode = "";
}

async function enter() {
    const current = app.value;
    if (!current) {
        return;
    }
    error.value = "";
    loading.value = true;
    try {
        await cleanupCurrentMicro();
        await nextTick();
        if (!microContainer.value) {
            throw new Error("微前端挂载容器未就绪");
        }
        const cleanup = await registerMicroApplication({
            name: `workspace:${current.code}`,
            path: current.basePath,
            entry: current.moduleEntry,
            styleEntry: current.styleEntry,
            appCode: current.code,
            theme: appStore.isDark ? "dark" : "light",
            locale: appStore.locale,
            user: {
                username: userStore.user?.username ?? "",
                realName: userStore.user?.realName,
            },
            getWorkspaceToken: () => userStore.token,
            navigate: (path) => {
                void router.push(path);
            },
            onSessionExpired: handleMicroSessionExpired,
            domElement: microContainer.value,
            onError: (message) => {
                if (!disposed) {
                    error.value = message;
                    emitMicroFrontendEvent("host.app.error", {
                        appCode: current.code,
                        path: current.basePath,
                        message,
                    });
                }
            },
        });
        if (disposed) {
            await cleanup();
            return;
        }
        cleanupMicro = cleanup;
        activeCode = current.code;
        emitMicroFrontendEvent("host.app.entered", {
            appCode: current.code,
            path: current.basePath,
        });
    } catch (e) {
        error.value =
            e instanceof Error ? e.message : "进入应用失败，请稍后重试";
    } finally {
        loading.value = false;
    }
}

async function load() {
    loading.value = true;
    try {
        await cleanupCurrentMicro();
        const basePath = route.meta.basePath;
        if (typeof basePath !== "string" || !basePath) {
            throw new Error("应用路由缺少 basePath");
        }
        app.value = await getAppRuntime(basePath);
        appStore.setActiveApp(app.value.name);
        await nextTick();
        if (app.value) {
            await enter();
        }
    } catch (e) {
        app.value = null;
        error.value = e instanceof Error ? e.message : "应用配置加载失败";
    } finally {
        loading.value = false;
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
onUnmounted(async () => {
    disposed = true;
    appStore.clearActiveApp();
    offMicroEvent?.();
    offMicroEvent = null;
    await cleanupCurrentMicro();
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
