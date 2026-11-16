<template>
    <template v-for="menu in menus" :key="menu.id">
        <el-sub-menu
            v-if="menu.children && menu.children.length"
            :index="menu.path || menu.id"
        >
            <template #title>
                <div class="menu-icon flex-cc">
                    <SvgIcon
                        v-if="menu.icon?.startsWith('ri:')"
                        :icon="menu.icon"
                    />
                    <el-icon v-else-if="iconOf(menu.icon)">
                        <component :is="iconOf(menu.icon)" />
                    </el-icon>
                </div>
                <span class="menu-name">{{ menu.menuName }}</span>
            </template>
            <SidebarMenu :menus="menu.children" />
        </el-sub-menu>
        <el-menu-item v-else :index="menu.path || menu.id">
            <div class="menu-icon flex-cc">
                <SvgIcon
                    v-if="menu.icon?.startsWith('ri:')"
                    :icon="menu.icon"
                />
                <el-icon v-else-if="iconOf(menu.icon)">
                    <component :is="iconOf(menu.icon)" />
                </el-icon>
            </div>
            <template #title>
                <span class="menu-name">{{ menu.menuName }}</span>
            </template>
        </el-menu-item>
    </template>
</template>

<script setup lang="ts">
import type { Component } from "vue";
import * as ElementPlusIcons from "@element-plus/icons-vue";
import SvgIcon from "@/components/core/base/svg-icon/index.vue";
import type { MenuNode } from "@/types/system";

defineOptions({ name: "SidebarMenu" });

defineProps<{ menus: MenuNode[] }>();

const iconMap = ElementPlusIcons as unknown as Record<string, Component>;

function iconOf(name?: string): Component | undefined {
    return name ? iconMap[name] : undefined;
}
</script>
