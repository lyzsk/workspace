<template>
    <div class="workspace-watermark">
        <el-watermark
            :content="[watermarkTitle, currentTime]"
            :font="{ fontSize: 16, color: 'rgba(128, 128, 128, 0.2)' }"
            :rotate="-22"
            :gap="[100, 100]"
            :offset="[50, 50]"
        >
            <div class="workspace-watermark-area"></div>
        </el-watermark>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useUserStore } from "@/store/modules/user";

const userStore = useUserStore();
const watermarkTitle = computed(() => {
    const user = userStore.user;
    return `Workspace - ${user?.realName || user?.username || "用户"}`;
});
const currentTime = ref(formatDateTime(new Date()));
let timer: number | undefined;

function formatDateTime(date: Date) {
    const pad = (value: number) => String(value).padStart(2, "0");
    const day = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
    const time = `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
    return `${day} ${time}`;
}

onMounted(() => {
    timer = window.setInterval(() => {
        currentTime.value = formatDateTime(new Date());
    }, 1000);
});

onUnmounted(() => window.clearInterval(timer));
</script>

<style scoped>
.workspace-watermark {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 3100;
    width: 100vw;
    height: 100vh;
    pointer-events: none;
}

.workspace-watermark-area {
    height: 100vh;
}
</style>
