<template>
    <div class="layout">
        <aside
            class="sidebar"
            :class="{
                collapsed: sidebarCollapsed && !isAppMode,
                'app-rail': isAppMode,
            }"
        >
            <div class="logo" @click="goHome">
                <img src="/apple-touch-icon.png" alt="Workspace" />
                <span v-if="!sidebarCollapsed && !isAppMode">{{
                    t("app.name")
                }}</span>
            </div>
            <el-scrollbar class="sidebar-scroll">
                <el-menu
                    v-if="!isAppMode"
                    :default-active="route.path"
                    router
                    class="menu el-menu-design"
                    :collapse="sidebarCollapsed"
                    :collapse-transition="false"
                >
                    <el-menu-item index="/dashboard">
                        <div class="menu-icon flex-cc">
                            <SvgIcon icon="ri:home-2-line" />
                        </div>
                        <template #title>
                            <span class="menu-name">{{
                                t("menu.dashboard")
                            }}</span>
                        </template>
                    </el-menu-item>
                    <SidebarMenu :menus="permissionStore.menus" />
                </el-menu>
                <el-menu
                    v-else
                    :default-active="route.path"
                    router
                    class="menu el-menu-design"
                    :collapse="true"
                    :collapse-transition="false"
                >
                    <el-menu-item
                        v-for="app in appEntries"
                        :key="app.id"
                        :index="app.path"
                    >
                        <div class="menu-icon flex-cc">
                            <SvgIcon
                                :icon="
                                    app.icon?.startsWith('ri:')
                                        ? app.icon
                                        : 'ri:apps-line'
                                "
                            />
                        </div>
                        <template #title>
                            <span class="menu-name">{{ app.name }}</span>
                        </template>
                    </el-menu-item>
                </el-menu>
            </el-scrollbar>
        </aside>

        <div class="main">
            <header class="header" :class="{ 'app-header': isAppMode }">
                    <div
                        v-if="isAppMode"
                        class="app-workspace-brand"
                        @click="goHome"
                    >
                        <img src="/apple-touch-icon.png" alt="Workspace" />
                        <span>{{ t("app.name") }}</span>
                    </div>
                    <div class="header-left">
                        <button
                            class="icon-btn"
                            :title="sidebarCollapsed ? '展开菜单' : '收起菜单'"
                            @click="sidebarCollapsed = !sidebarCollapsed"
                        >
                            <el-icon>
                                <Expand v-if="sidebarCollapsed" />
                                <Fold v-else />
                            </el-icon>
                        </button>
                        <button
                            class="icon-btn"
                            title="刷新"
                            @click="refreshPage"
                        >
                            <el-icon><Refresh /></el-icon>
                        </button>
                        <nav class="top-breadcrumb">
                            <template
                                v-for="(item, index) in breadcrumbs"
                                :key="`${item.title}-${index}`"
                            >
                                <a
                                    v-if="item.clickable && item.path"
                                    @click.prevent="router.push(item.path)"
                                >
                                    {{ item.title }}
                                </a>
                                <span v-else>{{ item.title }}</span>
                                <span
                                    v-if="index < breadcrumbs.length - 1"
                                    class="top-breadcrumb-sep"
                                >
                                    /
                                </span>
                            </template>
                        </nav>
                    </div>

                    <div class="actions">
                        <button
                            class="icon-btn"
                            :title="isFullscreen ? '退出全屏' : '全屏'"
                            :aria-label="isFullscreen ? '退出全屏' : '全屏'"
                            @click="toggleFullscreen"
                        >
                            <SvgIcon
                                :icon="
                                    isFullscreen
                                        ? 'ri:fullscreen-exit-line'
                                        : 'ri:fullscreen-line'
                                "
                            />
                        </button>
                        <button
                            class="icon-btn"
                            :title="
                                appStore.locale === 'zh-CN'
                                    ? 'English'
                                    : '简体中文'
                            "
                            :aria-label="
                                appStore.locale === 'zh-CN'
                                    ? 'English'
                                    : '简体中文'
                            "
                            @click="toggleLocale"
                        >
                            <svg viewBox="0 0 24 24" aria-hidden="true">
                                <path
                                    fill="currentColor"
                                    d="m18.5 10l4.4 11h-2.155l-1.201-3h-4.09l-1.199 3h-2.154L16.5 10zM10 2v2h6v2h-1.968a18.2 18.2 0 0 1-3.62 6.301a15 15 0 0 0 2.335 1.707l-.75 1.878A17 17 0 0 1 9 13.725a16.7 16.7 0 0 1-6.201 3.548l-.536-1.929a14.7 14.7 0 0 0 5.327-3.042A18 18 0 0 1 4.767 8h2.24A16 16 0 0 0 9 10.877a16.2 16.2 0 0 0 2.91-4.876L2 6V4h6V2zm7.5 10.885L16.253 16h2.492z"
                                />
                            </svg>
                        </button>
                        <button
                            class="icon-btn theme-btn"
                            :title="
                                appStore.isDark
                                    ? t('header.light')
                                    : t('header.dark')
                            "
                            :aria-label="
                                appStore.isDark
                                    ? t('header.light')
                                    : t('header.dark')
                            "
                            @click="toggleTheme"
                        >
                            <svg
                                v-if="appStore.isDark"
                                viewBox="0 0 24 24"
                                aria-hidden="true"
                            >
                                <path
                                    fill="currentColor"
                                    d="M12 18a6 6 0 1 1 0-12a6 6 0 0 1 0 12M11 1h2v3h-2zm0 19h2v3h-2zM3.515 4.929l1.414-1.414L7.05 5.636L5.636 7.05zM16.95 18.364l1.414-1.414l2.121 2.121l-1.414 1.414zm2.121-14.85l1.414 1.415l-2.121 2.121l-1.414-1.414zM5.636 16.95l1.414 1.414l-2.121 2.121l-1.414-1.414zM23 11v2h-3v-2zM4 11v2H1v-2z"
                                />
                            </svg>
                            <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                                <path
                                    fill="currentColor"
                                    d="M10 7a7 7 0 0 0 12 4.9v.1c0 5.523-4.477 10-10 10S2 17.523 2 12S6.477 2 12 2h.1A6.98 6.98 0 0 0 10 7m-6 5a8 8 0 0 0 15.062 3.762A9 9 0 0 1 8.238 4.938A8 8 0 0 0 4 12"
                                />
                            </svg>
                        </button>
                        <IconButton
                            icon="ri:notification-2-line"
                            class="notice-button"
                            @click="toggleNotice"
                        >
                            <span v-if="unreadCount" class="notice-dot"></span>
                        </IconButton>
                        <Notification v-model="noticeVisible" />
                        <UserMenu />
                    </div>
                </header>

                <WorkTabs
                    v-if="!isAppMode"
                    :title="currentTab.title"
                    :icon="currentTab.icon"
                />

            <main class="content" :class="{ 'content-full': isAppMode }">
                <router-view :key="dataRefreshKey" />
            </main>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { Expand, Fold, Refresh } from "@element-plus/icons-vue";
import SvgIcon from "@/components/core/base/svg-icon/index.vue";
import IconButton from "@/components/core/base/icon-button/index.vue";
import SidebarMenu from "@/components/core/layouts/sidebar-menu/index.vue";
import WorkTabs from "@/components/core/layouts/work-tabs/index.vue";
import UserMenu from "@/components/core/layouts/user-menu/index.vue";
import Notification from "@/components/core/layouts/notification/index.vue";
import { storeToRefs } from "pinia";
import { useAppStore } from "@/store/modules/app";
import { useNotificationStore } from "@/store/modules/notification";
import { usePermissionStore } from "@/store/modules/permission";
import type { MenuNode } from "@/types/system";
import { toggleThemeWithAnimation } from "@/utils/themeAnimation";
import { dataRefreshKey, applyPendingDataChange } from "@/utils/dataChange";
import { useIdleLogout } from "@/composables/useIdleLogout";
import { useSessionHeartbeat } from "@/composables/useSessionHeartbeat";
import { ElNotification } from "element-plus";
import { getMe } from "@/api/auth";
import { useUserStore } from "@/store/modules/user";

const { t } = useI18n();
const appStore = useAppStore();
const userStore = useUserStore();
const notificationStore = useNotificationStore();
const { unreadCount } = storeToRefs(notificationStore);
const noticeVisible = ref(false);
const permissionStore = usePermissionStore();
const route = useRoute();
const router = useRouter();

interface Crumb {
    title: string;
    path?: string;
    clickable?: boolean;
    icon?: string;
}

function findTrail(
    menus: MenuNode[],
    target: string,
    parents: MenuNode[] = [],
): MenuNode[] | null {
    for (const menu of menus) {
        const trail = [...parents, menu];
        if (menu.path === target) {
            return trail;
        }
        if (menu.children?.length) {
            const found = findTrail(menu.children, target, trail);
            if (found) {
                return found;
            }
        }
    }
    return null;
}

const breadcrumbs = computed<Crumb[]>(() => {
    if (isAppMode.value) {
        const result: Crumb[] = [{ title: t("app.name") }];
        if (appStore.activeAppName) {
            result.push({ title: appStore.activeAppName });
        }
        for (const title of appStore.activeAppPageTitles) {
            result.push({ title });
        }
        return result;
    }
    if (route.path === "/dashboard") {
        return [{ title: t("menu.dashboard"), icon: "HomeFilled" }];
    }
    const trail = findTrail(permissionStore.menus, route.path);
    if (trail) {
        return trail.map((menu) => ({
            title: menu.menuName,
            path: menu.path,
            clickable: menu.menuType === "MENU",
            icon: menu.icon || "Document",
        }));
    }
    const title = route.meta.title ? String(route.meta.title) : "";
    return title ? [{ title, icon: "Document" }] : [];
});

const sidebarCollapsed = ref(false);
const isAppMode = computed(() => String(route.name ?? "").startsWith("app:"));

interface AppEntry {
    id: string;
    name: string;
    path: string;
    icon?: string;
}

const appEntries = computed<AppEntry[]>(() => {
    const result: AppEntry[] = [];
    const walk = (items: MenuNode[]) => {
        for (const item of items) {
            if (item.menuCode?.startsWith("app:") && item.path) {
                result.push({
                    id: item.id,
                    name: item.menuName,
                    path: item.path,
                    icon: item.icon,
                });
            }
            if (item.children?.length) {
                walk(item.children);
            }
        }
    };
    walk(permissionStore.menus);
    return result;
});

function goHome() {
    router.push("/dashboard");
}
const currentTab = computed(
    () => breadcrumbs.value[breadcrumbs.value.length - 1] ?? { title: "" },
);
const isFullscreen = ref(false);

function refreshPage() {
    window.location.reload();
}

function syncFullscreen() {
    isFullscreen.value = !!document.fullscreenElement;
}

async function toggleFullscreen() {
    try {
        if (document.fullscreenElement) {
            await document.exitFullscreen();
        } else {
            await document.documentElement.requestFullscreen();
        }
    } catch {}
}

function toggleNotice() {
    noticeVisible.value = !noticeVisible.value;
}

function closeNoticeOnOutside(event: MouseEvent) {
    if (!noticeVisible.value) {
        return;
    }
    const target = event.target as HTMLElement;
    if (
        !target.closest(".notice-button") &&
        !target.closest(".notification-panel")
    ) {
        noticeVisible.value = false;
    }
}

function onVisibilityChange() {
    if (!document.hidden) {
        applyPendingDataChange();
    }
}

function onSessionExpired() {
    ElNotification({
        title: "登录已过期",
        message: "提交或切换页面时需要重新登录",
        type: "warning",
        duration: 0,
    });
}

async function onDataRefresh(event: Event) {
    const permissionChanged = (
        event as CustomEvent<{
            permissionChanged?: boolean;
        }>
    ).detail?.permissionChanged;
    if (permissionChanged) {
        try {
            userStore.setUser(await getMe());
            await permissionStore.loadAndBuild(router, true);
            try {
                await router.replace(router.currentRoute.value.fullPath);
            } catch (error) {
                if (
                    !(error instanceof Error) ||
                    error.name !== "NavigationDuplicated"
                ) {
                    throw error;
                }
            }
        } catch {
            ElNotification({
                title: "权限刷新失败",
                message: "请手动刷新页面获取最新权限",
                type: "error",
            });
        }
    }
    dataRefreshKey.value += 1;
}

useIdleLogout();
useSessionHeartbeat();

onMounted(() => {
    document.addEventListener("fullscreenchange", syncFullscreen);
    document.addEventListener("click", closeNoticeOnOutside);
    document.addEventListener("visibilitychange", onVisibilityChange);
    window.addEventListener("workspace:session-expired", onSessionExpired);
    window.addEventListener("workspace:data-refresh", onDataRefresh);
});

onUnmounted(() => {
    document.removeEventListener("fullscreenchange", syncFullscreen);
    document.removeEventListener("click", closeNoticeOnOutside);
    document.removeEventListener("visibilitychange", onVisibilityChange);
    window.removeEventListener("workspace:session-expired", onSessionExpired);
    window.removeEventListener("workspace:data-refresh", onDataRefresh);
});

function toggleLocale() {
    appStore.setLocale(appStore.locale === "zh-CN" ? "en" : "zh-CN");
}

function toggleTheme(event: MouseEvent) {
    toggleThemeWithAnimation(event, () => appStore.toggleTheme());
}
</script>

<style scoped lang="scss">
.layout {
    display: flex;
    height: 100vh;
    background: var(--app-bg);
    color: var(--text);
}

.sidebar {
    display: flex;
    flex-direction: column;
    width: 220px;
    border-right: 1px solid var(--border);
    background: var(--sidebar-bg);
    transition: width 0.2s;
}

.sidebar.collapsed {
    width: 64px;
}

.sidebar.app-rail {
    width: 64px;
}

.sidebar.app-rail .logo {
    justify-content: center;
    padding: 0;
    cursor: pointer;
}

.sidebar.collapsed .logo {
    justify-content: center;
    padding: 0;
}

.logo {
    display: flex;
    align-items: center;
    height: 56px;
    padding: 0 20px;
    border-bottom: 1px solid var(--border);
    font-size: 18px;
    font-weight: 700;
}

.menu {
    --el-menu-bg-color: transparent;
    --el-menu-text-color: var(--text-muted);
    --el-menu-active-color: var(--primary);
    --el-menu-hover-bg-color: var(--slider-bg);
    border-right: 0;
    background: transparent;
}

.main {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-width: 0;
}

.header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 56px;
    padding: 0 20px;
    border-bottom: 1px solid var(--border);
    background: var(--panel-bg);
}

.app-workspace-brand {
    display: flex;
    flex: 0 0 230px;
    align-items: center;
    align-self: stretch;
    gap: 10px;
    margin-left: -20px;
    padding-left: 20px;
    border-right: 1px solid var(--border);
    cursor: pointer;
}

.app-workspace-brand img {
    width: 32px;
    height: 32px;
}

.app-workspace-brand span {
    overflow: hidden;
    font-size: 16px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.app-header .header-left {
    flex: 1;
    padding-left: 20px;
}

.header-left {
    display: flex;
    align-items: center;
    gap: 4px;
    min-width: 0;
}

.top-breadcrumb {
    display: flex;
    align-items: center;
    margin-left: 10px;
    font-size: 14px;
    white-space: nowrap;
}

.top-breadcrumb a {
    color: var(--text-muted);
    cursor: pointer;
    text-decoration: none;
}

.top-breadcrumb a:hover {
    color: var(--primary);
}

.top-breadcrumb-sep {
    margin: 0 8px;
    color: var(--text-muted);
}

.page-title {
    font-size: 15px;
    font-weight: 600;
}

.actions {
    display: flex;
    align-items: center;
    gap: 6px;
}

.icon-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    padding: 0;
    border: 0;
    border-radius: 8px;
    background: transparent;
    color: var(--text-muted);
    cursor: pointer;
    transition:
        background 0.2s,
        color 0.2s;
}

.icon-btn:hover {
    background: var(--slider-bg);
    color: var(--text);
}

.icon-btn svg {
    display: block;
    width: 19px;
    height: 19px;
}

.icon-btn.theme-btn svg {
    width: 20px;
    height: 20px;
}

.notice-button {
    position: relative;
}

.notice-dot {
    position: absolute;
    top: 8px;
    right: 8px;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--art-error);
}

.user-name {
    margin-left: 8px;
    color: var(--text-muted);
    font-size: 13px;
}

.content {
    flex: 1;
    padding: 24px;
    overflow: auto;
}

.content.content-full {
    min-height: 0;
    padding: 0;
    overflow: hidden;
}
</style>
