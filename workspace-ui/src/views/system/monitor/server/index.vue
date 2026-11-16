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
import { getServerInfo, type ServerInfo } from "@/api/system/monitor";
import Table from "@/components/core/tables/table/index.vue";
import TableCard from "@/components/core/tables/table-card/index.vue";
import { formatTime } from "@/utils/format";

defineOptions({ name: "MonitorServer" });

const loading = ref(false);
const rows = ref<Array<{ label: string; value: string | number }>>([]);

async function load() {
    loading.value = true;
    try {
        const info: ServerInfo = await getServerInfo();
        rows.value = [
            { label: "主机名", value: info.hostName },
            { label: "操作系统", value: `${info.osName} ${info.osVersion}` },
            { label: "Java 版本", value: info.javaVersion },
            { label: "Java 厂商", value: info.javaVendor },
            { label: "CPU 核数", value: info.cpuCores },
            { label: "堆内存总量(MB)", value: info.totalMemoryMb },
            { label: "堆内存已用(MB)", value: info.usedMemoryMb },
            { label: "堆内存空闲(MB)", value: info.freeMemoryMb },
            { label: "堆内存最大可用(MB)", value: info.maxMemoryMb },
            { label: "非堆内存已用(MB)", value: info.nonHeapUsedMb },
            { label: "运行时长(秒)", value: info.uptimeSeconds },
            { label: "服务器时间", value: formatTime(info.serverTime) },
        ];
    } finally {
        loading.value = false;
    }
}

onMounted(load);
</script>
