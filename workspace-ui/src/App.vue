<template>
    <router-view />
    <WorkspaceWatermark v-if="showWatermark" />
</template>

<script setup lang="ts">
import { computed, watch } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAppStore } from "@/store/modules/app";
import { useUserStore } from "@/store/modules/user";
import WorkspaceWatermark from "@/components/core/others/workspace-watermark/index.vue";

const EXCLUDED_PATHS = new Set([
    "/login",
    "/devtools-blocked",
    "/403",
    "/404",
    "/500",
    "/result/success",
    "/result/fail",
]);

const route = useRoute();
const appStore = useAppStore();
const userStore = useUserStore();
const { locale } = useI18n();
const showWatermark = computed(() => userStore.isLogin && !EXCLUDED_PATHS.has(route.path));

watch(
    () => appStore.locale,
    (value) => {
        locale.value = value;
    },
    { immediate: true },
);
</script>
