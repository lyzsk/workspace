<template>
    <div class="system-page">
        <SearchCard v-show="showSearch">
            <div class="search-row">
                <div class="search-field">
                    <span class="search-label">用户名</span>
                    <el-input
                        v-model="query.username"
                        placeholder="请输入用户名"
                        clearable
                        style="width: 150px"
                        @keyup.enter="search"
                    />
                </div>
                <div class="search-field">
                    <span class="search-label">模块</span>
                    <el-input
                        v-model="query.module"
                        placeholder="请输入模块"
                        clearable
                        style="width: 150px"
                        @keyup.enter="search"
                    />
                </div>
                <div class="search-field">
                    <span class="search-label">状态</span>
                    <el-select
                        v-model="query.status"
                        placeholder="请选择状态"
                        clearable
                        style="width: 120px"
                    >
                        <el-option label="成功" :value="1" />
                        <el-option label="失败" :value="0" />
                    </el-select>
                </div>
                <div class="search-field">
                    <span class="search-label">时间范围</span>
                    <el-date-picker
                        v-model="dateRange"
                        type="datetimerange"
                        value-format="YYYY-MM-DDTHH:mm:ss"
                        start-placeholder="开始时间"
                        end-placeholder="结束时间"
                        style="width: 360px"
                    />
                </div>
                <div class="search-actions">
                    <el-button type="primary" @click="search">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                </div>
            </div>
        </SearchCard>

        <TableCard searchable v-model:show-search="showSearch" @refresh="load">
            <template #actions>
                <el-button v-permission="'sys:log:oper:list'" @click="exportData">
                    导出 Excel
                </el-button>
            </template>

            <template #default="{ stripe, border, headerBackground, size }">
<Table
                v-loading="loading"
                :data="rows"
               
                @sort-change="onSortChange"
            
                :stripe="stripe"
                :border="border"
                :class="[`table-size-${size}`, { 'no-header-bg': !headerBackground }]">
            <el-table-column prop="module" label="模块" width="120" />
            <el-table-column prop="operation" label="操作" min-width="140" />
            <el-table-column prop="username" label="用户" width="120" />
            <el-table-column label="状态" width="80">
                <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                        {{ row.status === 1 ? "成功" : "失败" }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="costTime" label="耗时(ms)" width="100" sortable="custom" />
            <el-table-column prop="ip" label="IP" width="140" />
            <el-table-column label="操作时间" width="170" prop="operTime" sortable="custom">
                <template #default="{ row }">{{ formatTime(row.operTime) }}</template>
            </el-table-column>
            <el-table-column label="详情" width="80" fixed="right" align="center" class-name="action-cell">
                <template #default="{ row }">
                        <ButtonTable
                            type="view"
                            icon="ri:file-search-line"
                            @click="showDetail(row)"
                        />
                </template>
            </el-table-column>
            </Table>
</template>
            <template #pagination>
                <el-pagination
                    v-model:current-page="query.pageNum"
                    v-model:page-size="query.pageSize"
                    :total="Number(total)"
                    layout="total, sizes, prev, pager, next"
                    background
                    @current-change="load"
                    @size-change="load"
                />
            </template>
        </TableCard>

        <el-dialog v-model="detailVisible" title="操作日志详情" width="720px">
            <el-descriptions :column="2" border>
                <el-descriptions-item label="模块">{{ current?.module }}</el-descriptions-item>
                <el-descriptions-item label="操作">{{ current?.operation }}</el-descriptions-item>
                <el-descriptions-item label="请求方式">{{ current?.requestMethod }}</el-descriptions-item>
                <el-descriptions-item label="请求地址">{{ current?.requestUri }}</el-descriptions-item>
                <el-descriptions-item label="IP">{{ current?.ip }}</el-descriptions-item>
                <el-descriptions-item label="耗时">{{ current?.costTime }} ms</el-descriptions-item>
                <el-descriptions-item label="参数" :span="2">
                    <pre class="mono">{{ current?.requestParams || "-" }}</pre>
                </el-descriptions-item>
                <el-descriptions-item label="返回" :span="2">
                    <pre class="mono">{{ current?.responseResult || "-" }}</pre>
                </el-descriptions-item>
                <el-descriptions-item v-if="current?.errorMsg" label="错误" :span="2">
                    <pre class="mono">{{ current?.errorMsg }}</pre>
                </el-descriptions-item>
            </el-descriptions>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { exportOperLogs, pageOperLogs } from "@/api/system/log";
import ButtonTable from "@/components/core/tables/button-table/index.vue";
import SearchCard from "@/components/core/tables/search-card/index.vue";
import Table from "@/components/core/tables/table/index.vue";
import TableCard from "@/components/core/tables/table-card/index.vue";
import type { OperLogRow } from "@/types/system";
import { formatTime } from "@/utils/format";

defineOptions({ name: "SystemOperLog" });

const showSearch = ref(false);
const loading = ref(false);
const rows = ref<OperLogRow[]>([]);
const total = ref(0);
const dateRange = ref<[string, string] | null>(null);
const detailVisible = ref(false);
const current = ref<OperLogRow | null>(null);

const query = reactive({
    pageNum: 1,
    pageSize: 10,
    username: "",
    module: "",
    status: undefined as number | undefined,
    sortBy: undefined as string | undefined,
    order: undefined as string | undefined,
});

async function load() {
    loading.value = true;
    try {
        const result = await pageOperLogs({
            pageNum: query.pageNum,
            pageSize: query.pageSize,
            username: query.username || undefined,
            module: query.module || undefined,
            status: query.status,
            beginTime: dateRange.value?.[0],
            endTime: dateRange.value?.[1],
            sortBy: query.sortBy,
            order: query.order,
        });
        rows.value = result.records;
        total.value = Number(result.total);
    } finally {
        loading.value = false;
    }
}

function search() {
    query.pageNum = 1;
    load();
}

function onSortChange({
    prop,
    order,
}: {
    prop: string;
    order: "ascending" | "descending" | null;
}) {
    query.sortBy = order ? prop : undefined;
    query.order =
        order === "ascending" ? "asc" : order === "descending" ? "desc" : undefined;
    query.pageNum = 1;
    load();
}

function reset() {
    query.username = "";
    query.module = "";
    query.status = undefined;
    query.sortBy = undefined;
    query.order = undefined;
    query.pageNum = 1;
    dateRange.value = null;
    load();
}

function showDetail(row: OperLogRow) {
    current.value = row;
    detailVisible.value = true;
}

async function exportData() {
    await exportOperLogs({
        username: query.username || undefined,
        module: query.module || undefined,
        status: query.status,
        beginTime: dateRange.value?.[0],
        endTime: dateRange.value?.[1],
        sortBy: query.sortBy,
        order: query.order,
    });
}

onMounted(load);
</script>
