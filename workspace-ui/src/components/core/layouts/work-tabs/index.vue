<template>
    <div class="work-tabs">
        <div class="work-tabs-scroll">
            <div
                v-for="tab in tabs"
                :key="tab.path"
                class="work-tab"
                :class="{ active: tab.path === route.fullPath }"
                @click="router.push(tab.path)"
            >
                <el-icon v-if="iconOf(tab.icon)" class="work-tab-icon">
                    <component :is="iconOf(tab.icon)" />
                </el-icon>
                <span>{{ tab.title }}</span>
                <span
                    v-if="tab.closable"
                    class="work-tab-close"
                    @click.stop="closeTab(tab)"
                >
                    <el-icon><Close /></el-icon>
                </span>
            </div>
        </div>
        <el-dropdown trigger="click" @command="handleCommand">
            <button type="button" class="work-tabs-more">
                <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
                <el-dropdown-menu>
                    <el-dropdown-item command="refresh">刷新</el-dropdown-item>
                    <el-dropdown-item command="closeOthers" divided>
                        关闭其他
                    </el-dropdown-item>
                    <el-dropdown-item command="closeAll">
                        关闭全部
                    </el-dropdown-item>
                </el-dropdown-menu>
            </template>
        </el-dropdown>
    </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { Component } from "vue";
import * as ElementPlusIcons from "@element-plus/icons-vue";
import { ArrowDown, Close } from "@element-plus/icons-vue";

interface Tab {
    path: string;
    title: string;
    icon?: string;
    closable: boolean;
}

const props = defineProps<{
    title: string;
    icon?: string;
}>();

const route = useRoute();
const router = useRouter();
const iconMap = ElementPlusIcons as unknown as Record<string, Component>;
const tabs = ref<Tab[]>(loadTabs());

function iconOf(name?: string): Component | undefined {
    return name ? iconMap[name] : undefined;
}

function loadTabs(): Tab[] {
    try {
        const saved = JSON.parse(
            localStorage.getItem("workspace-work-tabs") || "[]",
        ) as Tab[];
        if (saved.length) {
            return saved;
        }
    } catch {
    }
    return [
        {
            path: "/dashboard",
            title: "工作台",
            icon: "HomeFilled",
            closable: false,
        },
    ];
}

function saveTabs() {
    localStorage.setItem("workspace-work-tabs", JSON.stringify(tabs.value));
}

watch(
    () => [route.fullPath, props.title, props.icon],
    () => {
        if (route.path === "/login" || route.name === "NotFound" || !props.title) {
            return;
        }
        const existing = tabs.value.find(
            (tab) => tab.path === route.fullPath,
        );
        if (existing) {
            existing.title = props.title;
            existing.icon = props.icon;
        } else {
            tabs.value.push({
                path: route.fullPath,
                title: props.title,
                icon: props.icon,
                closable: route.path !== "/dashboard",
            });
        }
        saveTabs();
    },
    { immediate: true },
);

function closeTab(tab: Tab) {
    const index = tabs.value.findIndex((item) => item.path === tab.path);
    if (index < 0) {
        return;
    }
    tabs.value.splice(index, 1);
    if (tab.path === route.fullPath) {
        const target =
            tabs.value[Math.min(index, tabs.value.length - 1)] ??
            tabs.value[0];
        router.push(target?.path ?? "/dashboard");
    }
    saveTabs();
}

function handleCommand(command: string) {
    if (command === "refresh") {
        window.location.reload();
        return;
    }
    if (command === "closeOthers") {
        tabs.value = tabs.value.filter(
            (tab) =>
                !tab.closable || tab.path === route.fullPath,
        );
    }
    if (command === "closeAll") {
        tabs.value = tabs.value.filter((tab) => !tab.closable);
        if (route.path !== "/dashboard") {
            router.push("/dashboard");
        }
    }
    saveTabs();
}
</script>

<style scoped>
.work-tabs {
    display: flex;
    align-items: center;
    gap: 8px;
    height: 48px;
    padding: 0 20px;
    border-bottom: 1px solid var(--border);
    background: var(--panel-bg);
}

.work-tabs-scroll {
    display: flex;
    flex: 1;
    align-items: center;
    min-width: 0;
    overflow-x: auto;
}

.work-tab {
    display: inline-flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    gap: 6px;
    height: 32px;
    padding: 0 10px;
    margin-right: 6px;
    border: 1px solid var(--border);
    border-radius: 8px;
    background: var(--slider-bg);
    color: var(--text-muted);
    font-size: 12px;
    line-height: 1;
    white-space: nowrap;
    cursor: pointer;
    transition:
        color 0.2s,
        border-color 0.2s,
        background 0.2s;
}

.work-tab:hover {
    color: var(--primary);
}

.work-tab.active {
    border-color: var(--primary);
    background: rgb(64 158 255 / 10%);
    color: var(--primary);
}

.work-tab-icon {
    font-size: 14px;
}

.work-tab-close {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    margin-left: 2px;
    border-radius: 50%;
    font-size: 10px;
}

.work-tab-close:hover {
    background: var(--border);
    color: var(--text);
}

.work-tabs-more {
    display: inline-flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    padding: 0;
    border: 1px solid var(--border);
    border-radius: 8px;
    background: var(--slider-bg);
    color: var(--text-muted);
    cursor: pointer;
}

.work-tabs-more:hover {
    color: var(--primary);
}
</style>
