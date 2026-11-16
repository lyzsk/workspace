<template>
    <div class="table-toolbar">
        <div class="table-toolbar-left">
            <slot name="actions" />
        </div>
        <div class="table-toolbar-right">
            <div
                class="button"
                :title="fullscreen ? '退出全屏' : '全屏'"
                @click="fullscreen = !fullscreen"
            >
                <SvgIcon
                    :icon="
                        fullscreen
                            ? 'ri:fullscreen-exit-line'
                            : 'ri:fullscreen-line'
                    "
                />
            </div>
            <div
                v-if="searchable"
                class="button"
                :class="{ active: showSearchModel }"
                title="查询"
                @click="showSearchModel = !showSearchModel"
            >
                <SvgIcon icon="ri:search-line" />
            </div>
            <div class="button" title="刷新" @click="emit('refresh')">
                <SvgIcon icon="ri:refresh-line" />
            </div>
            <!--
            <el-dropdown trigger="click" @command="handleTableSizeChange">
                <div class="button" title="表格密度">
                    <SvgIcon icon="ri:arrow-up-down-fill" />
                </div>
                <template #dropdown>
                    <el-dropdown-menu>
                        <div
                            v-for="item in tableSizeOptions"
                            :key="item.value"
                            class="table-size-btn-item"
                        >
                            <el-dropdown-item
                                :command="item.value"
                                :class="{
                                    'is-active-size': tableSize === item.value,
                                }"
                            >
                                {{ item.label }}
                            </el-dropdown-item>
                        </div>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
            -->
            <el-dropdown trigger="click" :hide-on-click="false">
                <div class="button" title="表格设置">
                    <SvgIcon icon="ri:align-right" />
                </div>
                <template #dropdown>
                    <el-dropdown-menu>
                        <el-dropdown-item>
                            <el-checkbox v-model="stripe">斑马纹</el-checkbox>
                        </el-dropdown-item>
                        <el-dropdown-item>
                            <el-checkbox v-model="border">边框</el-checkbox>
                        </el-dropdown-item>
                        <el-dropdown-item>
                            <el-checkbox v-model="headerBackground">表头背景</el-checkbox>
                        </el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
            <slot name="tools" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import SvgIcon from "@/components/core/base/svg-icon/index.vue";

type TableSize = "small" | "default" | "large";

interface TableOptions {
    stripe: boolean;
    border: boolean;
    headerBackground: boolean;
    size: TableSize;
}

const props = withDefaults(
    defineProps<{
        searchable?: boolean;
        showSearch?: boolean;
    }>(),
    {
        searchable: false,
        showSearch: false,
    },
);

const emit = defineEmits<{
    (e: "refresh"): void;
    (e: "update:showSearch", value: boolean): void;
    (e: "update:options", value: TableOptions): void;
    (e: "fullscreen", value: boolean): void;
}>();

const fullscreen = ref(false);
const stripe = ref(true);
const border = ref(false);
const headerBackground = ref(true);
const tableSize = ref<TableSize>("default");
const showSearchModel = computed({
    get: () => props.showSearch,
    set: (value: boolean) => emit("update:showSearch", value),
});

watch(fullscreen, (value) => emit("fullscreen", value));
watch(
    [stripe, border, headerBackground, tableSize],
    () => {
        emit("update:options", {
            stripe: stripe.value,
            border: border.value,
            headerBackground: headerBackground.value,
            size: tableSize.value,
        });
    },
    { immediate: true },
);

</script>

<style scoped>
.table-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
}

.table-toolbar-left,
.table-toolbar-right {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
}

.button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 30px;
    border-radius: 6px;
    color: var(--text-muted);
    font-size: 16px;
    cursor: pointer;
    transition:
        background 0.2s,
        color 0.2s;
}

.button:hover,
.button.active {
    background: var(--slider-bg);
    color: var(--primary);
}

.table-size-btn-item :deep(.el-dropdown-menu__item.is-active-size) {
    background: var(--slider-bg);
    color: var(--primary);
}
</style>
