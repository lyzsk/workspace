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
import { useUserStore } from "@/store/modules/user";
import { registerMicroApplication } from "@/utils/singleSpa";
import { emitMicroFrontendEvent } from "@/utils/microFrontendEvents";

defineOptions({ name: "MicroFrontendHost" });

const route = useRoute();
const userStore = useUserStore();
const app = ref<AppInfo | null>(null);
const loading = ref(false);
const error = ref("");
const microContainer = ref<HTMLElement>();

let disposed = false;
let activeCode = "";
let cleanupMicro: (() => Promise<void>) | null = null;

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
            domElement: microContainer.value,
            workspaceToken: userStore.token,
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
        app.value = await getAppRuntime(route.path);
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

watch(() => route.path, () => {
    if (!disposed) {
        load();
    }
});

onMounted(load);
onUnmounted(async () => {
    disposed = true;
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

.app-error {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    gap: 12px;
}
</style>
