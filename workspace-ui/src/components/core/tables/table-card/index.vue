<template>
    <section
        class="table-card"
        :class="{ 'is-fullscreen': fullscreen }"
    >
        <TableHeader
            :searchable="searchable"
            :show-search="showSearch"
            @refresh="emit('refresh')"
            @update:show-search="emit('update:showSearch', $event)"
            @update:options="options = $event"
            @fullscreen="fullscreen = $event"
        >
            <template #actions>
                <slot name="actions" />
            </template>
            <template #tools>
                <slot name="tools" />
            </template>
        </TableHeader>
        <slot v-bind="options" />
        <div v-if="$slots.pagination" class="table-pagination">
            <slot name="pagination" />
        </div>
    </section>
</template>

<script setup lang="ts">
import { ref } from "vue";
import TableHeader from "@/components/core/tables/table-header/index.vue";

interface TableOptions {
    stripe: boolean;
    border: boolean;
    headerBackground: boolean;
    size: "small" | "default" | "large";
}

withDefaults(
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
}>();

const fullscreen = ref(false);
const options = ref<TableOptions>({
    stripe: true,
    border: false,
    headerBackground: true,
    size: "default",
});
</script>

<style scoped>
.table-card.is-fullscreen {
    position: fixed;
    inset: 0;
    z-index: 2500;
    margin: 0;
    border-radius: 0;
    overflow: auto;
}

.table-pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 14px;
}
</style>
