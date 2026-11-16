<template>
    <div class="art-table" :class="`table-size-${size}`">
        <el-table
            v-bind="$attrs"
            :stripe="stripe"
            :border="border"
            :class="{ 'no-header-bg': !headerBackground }"
        >
            <slot />
        </el-table>
        <div v-if="$slots.pagination" class="table-pagination">
            <slot name="pagination" />
        </div>
    </div>
</template>

<script setup lang="ts">
defineOptions({ name: "Table", inheritAttrs: false });

withDefaults(
    defineProps<{
        stripe?: boolean;
        border?: boolean;
        headerBackground?: boolean;
        size?: "small" | "default" | "large";
    }>(),
    {
        stripe: true,
        border: false,
        headerBackground: true,
        size: "default",
    },
);
</script>

<style src="./style.scss" scoped></style>
