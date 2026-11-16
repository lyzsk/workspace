<template>
    <div class="api-doc-page">
        <iframe
            ref="frameRef"
            class="api-doc-frame"
            :src="docUrl"
            title="Workspace 接口文档"
            @load="applyTheme"
        ></iframe>
    </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { useAppStore } from "@/store/modules/app";

const docUrl = import.meta.env.VITE_API_DOC_URL || "/doc.html";
const frameRef = ref<HTMLIFrameElement>();
const appStore = useAppStore();
const styleId = "workspace-api-doc-theme";
const footerHidden = `
    .globalFooter, .knife4j-footer, a[href*="gitee.com/xiaoym/knife4j"] {
        display: none !important;
    }
`;

function applyTheme() {
    try {
        const frameDocument = frameRef.value?.contentDocument;
        if (!frameDocument?.head) {
            return;
        }
        const dark = appStore.isDark;
        frameDocument.documentElement.classList.toggle("workspace-dark", dark);
        const primary = getComputedStyle(document.documentElement)
            .getPropertyValue("--primary")
            .trim() || "#5d87ff";
        let style = frameDocument.getElementById(styleId) as HTMLStyleElement | null;
        if (!style) {
            style = frameDocument.createElement("style");
            style.id = styleId;
            frameDocument.head.appendChild(style);
        }
        style.textContent =
            footerHidden + (dark ? darkTheme(primary) : lightTheme(primary));
    } catch {
    }
}

function lightTheme(primary: string) {
    return `
        :root { --workspace-primary: ${primary}; }
        a, .ant-btn-link, .ant-tabs-tab-active .ant-tabs-tab-btn { color: var(--workspace-primary) !important; }
        .ant-btn-primary { border-color: var(--workspace-primary) !important; background: var(--workspace-primary) !important; }
    `;
}

function darkTheme(primary: string) {
    return `
        :root { --workspace-primary: ${primary}; color-scheme: dark; }
        html, body, #app, .ant-layout, .ant-layout-content, .main-content, .content {
            background: #070707 !important; color: #e3e3e8 !important;
        }
        body, .ant-layout, .ant-card, .ant-table, .ant-list, .ant-descriptions,
        .ant-tabs, .ant-collapse, .ant-modal-content, .ant-modal-header,
        .ant-drawer-content, .ant-dropdown-menu, .ant-select-dropdown {
            background: #161618 !important; color: #e3e3e8 !important;
        }
        .ant-layout-header, .ant-card-head, .ant-tabs-nav, .ant-modal-header,
        .knife4j-header-default, .knife4j-footer, .ant-layout-footer {
            background: #161618 !important; color: #e3e3e8 !important;
            border-color: rgba(255,255,255,.1) !important;
        }
        .ant-tabs-tab-active, .ant-select-selection {
            background: #101014 !important; color: #e3e3e8 !important;
            border-color: rgba(255,255,255,.16) !important;
        }
        .ant-tabs-tab {
            color: #ababba !important;
            background: #161618 !important;
            border-color: rgba(255,255,255,.12) !important;
        }
        .ant-tabs-tab .ant-tabs-tab-btn, .ant-tabs-tab span {
            color: #ababba !important;
        }
        .ant-tabs-tab:hover, .ant-tabs-tab:hover .ant-tabs-tab-btn,
        .ant-tabs-tab:hover span {
            color: #e3e3e8 !important;
        }
        .ant-tabs-tab-active .ant-tabs-tab-btn, .ant-tabs-tab-active span {
            color: var(--workspace-primary) !important;
        }
        .ant-tabs-close-x, .ant-tabs-tab .anticon-close {
            color: #ababba !important;
        }
        .ant-tabs-close-x:hover, .ant-tabs-tab .anticon-close:hover {
            color: #e3e3e8 !important;
        }
        .ant-select-selection-selected-value, .ant-select-selection__rendered,
        .knife4j-header-default .anticon {
            color: #e3e3e8 !important;
        }
        .ant-layout-sider, .ant-menu, .ant-menu-sub, .ant-menu-inline {
            background: #101014 !important; color: #ababba !important;
        }
        .ant-menu-item, .ant-menu-title-content, .ant-menu-submenu-title {
            color: #ababba !important;
        }
        .ant-menu-item-selected, .ant-menu-item-active, .ant-menu-submenu-active {
            background: rgba(93,135,255,.18) !important; color: var(--workspace-primary) !important;
        }
        h1, h2, h3, h4, h5, h6, p, label, .ant-card-head-title, .ant-tabs-tab,
        .ant-descriptions-item-label, .ant-descriptions-item-content,
        .ant-table-title, .ant-table-footer, .ant-list-item-meta-title,
        .ant-list-item-meta-description, .ant-typography, .ant-collapse-header,
        .ant-modal-title, .ant-drawer-title, .ant-empty-description {
            color: #e3e3e8 !important; border-color: rgba(255,255,255,.1) !important;
        }
        span, td, th, .ant-table-cell, .ant-descriptions-item, .ant-list-item {
            border-color: rgba(255,255,255,.1) !important;
        }
        .ant-table-thead > tr > th, .ant-table-tbody > tr > td,
        .ant-table-tbody > tr.ant-table-row:hover > td, .ant-table-cell-row-hover {
            background: #161618 !important; color: #e3e3e8 !important;
            border-color: rgba(255,255,255,.1) !important;
        }
        .ant-input, .ant-input-affix-wrapper, .ant-select-selector, .ant-input-number,
        textarea, .ant-select-selection-search-input {
            background: #101014 !important; color: #e3e3e8 !important;
            border-color: rgba(255,255,255,.16) !important;
        }
        .ant-input::placeholder, textarea::placeholder, .ant-select-selection-placeholder,
        .ant-select-arrow, .ant-select-selection-item, .ant-btn-default,
        .ant-collapse-content, .ant-collapse-content-box, .ant-dropdown-menu-item {
            color: #ababba !important; background: #161618 !important;
            border-color: rgba(255,255,255,.12) !important;
        }
        .ant-btn-primary { border-color: var(--workspace-primary) !important;
            background: var(--workspace-primary) !important; color: #fff !important; }
        a, .ant-btn-link, .ant-tabs-tab-active .ant-tabs-tab-btn {
            color: var(--workspace-primary) !important;
        }
        .ant-tag, .ant-alert, .ant-statistic-content, .ant-badge-count {
            color: #e3e3e8 !important;
        }
        .ant-divider, .ant-card-bordered, .ant-tabs-top-bar,
        .ant-descriptions-view, .ant-list-split .ant-list-item {
            border-color: rgba(255,255,255,.1) !important;
        }
        ::-webkit-scrollbar { width: 8px; height: 8px; }
        ::-webkit-scrollbar-track { background: #101014; }
        ::-webkit-scrollbar-thumb { background: #3a3a42; border-radius: 8px; }
    `;
}

watch(() => appStore.isDark, applyTheme);
</script>

<style scoped lang="scss">
.api-doc-page {
    width: 100%;
    height: 100%;
    min-height: calc(100vh - 150px);
    overflow: hidden;
    border: 1px solid var(--border);
    border-radius: var(--custom-radius);
    background: var(--panel-bg);
}

.api-doc-frame {
    display: block;
    width: 100%;
    height: 100%;
    min-height: calc(100vh - 150px);
    border: 0;
    background: var(--panel-bg);
}
</style>
