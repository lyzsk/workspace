<template>
    <ElConfigProvider :z-index="2000" :locale="zhCn" size="default">
        <div class="sub-app-root" :data-theme="theme" :data-mode="mode">
            <RouterView />
        </div>
    </ElConfigProvider>
</template>

<script setup lang="ts">
import { RouterView } from "vue-router";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import { useTheme } from "@/micro/theme";
import { getRunMode } from "@/micro/context";

/**
 * 根组件。
 *
 * 只做三件事：包裹 Element Plus 的全局配置、提供样式作用域根节点、输出路由视图。
 * 不在这里放侧边栏、顶栏或任何框架级布局 —— 那些由主应用提供，
 * 子应用再放一套会出现「双菜单」。
 *
 * 语言固定为中文。接入多语言时，把 locale 换成根据宿主 props.locale 计算的值。
 */

const theme = useTheme();

/**
 * 运行模式，写到根节点上供样式使用。
 *
 * 嵌入宿主时留白统一由宿主控制，子应用不再自己加内边距，否则两层间距叠在一起；
 * 独立运行时没有宿主容器，仍然需要自己的内边距。
 */
const mode = getRunMode();
</script>
