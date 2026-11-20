<template>
    <div class="system-page">
        <TableCard @refresh="load">
            <template #default="{ stripe, border, headerBackground, size }">
                <Table
                    v-loading="loading"
                    :data="rows"
                    :stripe="stripe"
                    :border="border"
                    :class="[
                        `table-size-${size}`,
                        { 'no-header-bg': !headerBackground },
                    ]"
                >
                    <el-table-column prop="label" label="指标" min-width="180" />
                    <el-table-column prop="value" label="当前值" min-width="260" />
                </Table>
            </template>
        </TableCard>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { getCacheStats } from "@/api/system/monitor";
import Table from "@/components/core/tables/table/index.vue";
import TableCard from "@/components/core/tables/table-card/index.vue";

defineOptions({ name: "MonitorCache" });

const loading = ref(false);
const rows = ref<Array<{ label: string; value: string | number }>>([]);

async function load() {
    loading.value = true;
    try {
        const stats = await getCacheStats();
        rows.value = [
            { label: "缓存实现", value: stats.engine },
            { label: "当前库条目数", value: stats.size },
            { label: "Redis 全局命中次数", value: stats.hits },
            { label: "Redis 全局未命中次数", value: stats.misses },
            { label: "Redis 全局命中率", value: `${(stats.hitRate * 100).toFixed(2)}%` },
        ];
    } finally {
        loading.value = false;
    }
}

onMounted(load);
</script>
