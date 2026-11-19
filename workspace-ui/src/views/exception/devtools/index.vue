<template>
    <div class="devtools-blocked">
        <el-result icon="warning" title="页面访问受限" sub-title="正在返回安全页面…">
            <template #extra>
                <el-button type="primary" @click="returnToSafePage">
                    立即返回
                </el-button>
            </template>
        </el-result>
    </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from "vue";
import { useUserStore } from "@/store/modules/user";

const userStore = useUserStore();
let timer: number | undefined;

function returnToSafePage() {
    window.location.replace(userStore.isLogin ? "/dashboard" : "/login");
}

onMounted(() => {
    timer = window.setTimeout(returnToSafePage, 300);
});

onUnmounted(() => window.clearTimeout(timer));
</script>

<style scoped>
.devtools-blocked {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    background: var(--app-bg, var(--el-bg-color));
    color: var(--text, var(--el-text-color-primary));
}
</style>
