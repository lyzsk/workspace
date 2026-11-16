<template>
    <div class="system-page">
        <TableCard @refresh="load">
            <template #actions>
                <el-button type="primary" @click="load">刷新</el-button>
                <span class="hint">共 {{ rows.length }} 个会话</span>
            </template>

            <template #default="{ stripe, border, headerBackground, size }">
<Table v-loading="loading" :data="rows"
                :stripe="stripe"
                :border="border"
                :class="[`table-size-${size}`, { 'no-header-bg': !headerBackground }]">
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="realName" label="姓名" min-width="100" />
            <el-table-column prop="ip" label="IP" width="140" />
            <el-table-column prop="userAgent" label="User-Agent" min-width="240" />
            <el-table-column label="登录时间" width="170" prop="loginTime" sortable>
                <template #default="{ row }">{{ formatTime(row.loginTime) }}</template>
            </el-table-column>
            <el-table-column label="最后活跃时间" width="170">
                <template #default="{ row }">{{ formatTime(row.lastSeenAt) }}</template>
            </el-table-column>
            <el-table-column label="过期时间" width="170">
                <template #default="{ row }">{{ formatTime(row.expiresAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right" align="center" class-name="action-cell">
                <template #default="{ row }">
                        <ButtonTable
                            v-permission="'sys:online:kick'"
                            type="delete"
                            icon="ri:shut-down-line"
                            @click="kick(row)"
                        />
                </template>
            </el-table-column>
            </Table>
</template>
        </TableCard>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { kickOnlineUser, listOnlineUsers } from "@/api/system/online";
import ButtonTable from "@/components/core/tables/button-table/index.vue";
import Table from "@/components/core/tables/table/index.vue";
import TableCard from "@/components/core/tables/table-card/index.vue";
import type { OnlineUserRow } from "@/types/system";
import { formatTime } from "@/utils/format";

defineOptions({ name: "SystemOnline" });

const loading = ref(false);
const rows = ref<OnlineUserRow[]>([]);

async function load() {
    loading.value = true;
    try {
        rows.value = await listOnlineUsers();
    } finally {
        loading.value = false;
    }
}

async function kick(row: OnlineUserRow) {
    await ElMessageBox.confirm(
        `确定强制「${row.username}」下线吗？该用户所有 token 会立即失效。`,
        "提示",
        { type: "warning" },
    );
    await kickOnlineUser(row.userId);
    ElMessage.success("已强制下线");
    load();
}

onMounted(load);
</script>

<style scoped>
.hint {
    color: var(--text-muted);
    font-size: 13px;
}
</style>
