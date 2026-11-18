<template>
    <div class="system-page">
        <SearchCard v-show="showSearch">
            <div class="search-row">
                <div class="search-field">
                    <span class="search-label">任务名称</span>
                    <el-input
                        v-model="query.jobName"
                        placeholder="请输入任务名称"
                        clearable
                        style="width: 180px"
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
                        <el-option label="运行" :value="1" />
                        <el-option label="暂停" :value="0" />
                    </el-select>
                </div>
                <div class="search-actions">
                    <el-button type="primary" @click="search">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                </div>
            </div>
        </SearchCard>

        <TableCard searchable v-model:show-search="showSearch" @refresh="load">
            <template #actions>
                <el-button
                    v-permission="'sys:job:create'"
                    type="primary"
                    @click="openCreate"
                >
                    新增任务
                </el-button>
            </template>
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
                    <el-table-column prop="jobName" label="任务名称" min-width="150" />
                    <el-table-column prop="jobGroup" label="任务分组" width="100" />
                    <el-table-column prop="cronExpression" label="cron 表达式" min-width="150" />
                    <el-table-column prop="handlerName" label="调用处理器" min-width="130" />
                    <el-table-column label="状态" width="90">
                        <template #default="{ row }">
                            <el-switch
                                :model-value="row.status === 1"
                                @change="toggleStatus(row)"
                            />
                        </template>
                    </el-table-column>
                    <el-table-column label="创建时间" width="165" prop="createTime" sortable="custom">
                        <template #default="{ row }">
                            {{ formatTime(row.createTime) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="140" fixed="right" align="center" class-name="action-cell">
                        <template #default="{ row }">
                            <ButtonTable
                                v-permission="'sys:job:update'"
                                type="edit"
                                @click="openEdit(row)"
                            />
                            <ButtonTable
                                v-permission="'sys:job:delete'"
                                type="delete"
                                @click="remove(row)"
                            />
                            <ButtonMore
                                :list="moreItems"
                                @click="(item) => handleMore(item.key, row)"
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

        <el-dialog
            v-model="dialogVisible"
            :title="editingId ? '修改任务' : '新增任务'"
            width="620px"
        >
            <el-form :model="form" label-width="100px">
                <el-form-item label="任务名称" required>
                    <el-input v-model.trim="form.jobName" />
                </el-form-item>
                <el-form-item label="任务分组">
                    <el-input v-model.trim="form.jobGroup" />
                </el-form-item>
                <el-form-item label="cron 表达式" required>
                    <CronExpressionBuilder v-model="form.cronExpression" />
                </el-form-item>
                <el-form-item label="调用处理器" required>
                    <el-input v-model.trim="form.handlerName" placeholder="demoJob" />
                </el-form-item>
                <el-form-item label="参数">
                    <el-input v-model.trim="form.params" />
                </el-form-item>
                <el-form-item label="失火策略">
                    <el-select v-model="form.misfirePolicy" style="width: 100%">
                        <el-option label="忽略失火，立即执行" :value="0" />
                        <el-option label="失火后补跑一次" :value="1" />
                        <el-option label="放弃失火，等待下次" :value="2" />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch
                        v-model="form.status"
                        :active-value="1"
                        :inactive-value="0"
                        active-text="运行"
                        inactive-text="暂停"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="dialogVisible = false">取消</el-button>
                    <el-button type="primary" :loading="saving" @click="save">保存</el-button>
                </div>
            </template>
        </el-dialog>

        <el-dialog v-model="logVisible" title="调度日志" width="1000px">
            <TableCard @refresh="loadLogs">
                <template #default="{ stripe, border, headerBackground, size }">
                    <Table
                        v-loading="logLoading"
                        :data="logs"
                        :stripe="stripe"
                        :border="border"
                        :class="[
                            `table-size-${size}`,
                            { 'no-header-bg': !headerBackground },
                        ]"
                    >
                        <el-table-column prop="jobName" label="任务名称" min-width="140" />
                        <el-table-column prop="handlerName" label="处理器" min-width="120" />
                        <el-table-column label="状态" width="80">
                            <template #default="{ row }">
                                <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                                    {{ row.status === 1 ? "成功" : "失败" }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="costTime" label="耗时(ms)" width="100" />
                        <el-table-column label="开始时间" width="165">
                            <template #default="{ row }">
                                {{ formatTime(row.startTime) }}
                            </template>
                        </el-table-column>
                        <el-table-column label="结束时间" width="165">
                            <template #default="{ row }">
                                {{ formatTime(row.endTime) }}
                            </template>
                        </el-table-column>
                        <el-table-column prop="errorMsg" label="异常信息" min-width="160" />
                    </Table>
                </template>
                <template #pagination>
                    <el-pagination
                        v-model:current-page="logQuery.pageNum"
                        v-model:page-size="logQuery.pageSize"
                        :total="Number(logTotal)"
                        layout="total, sizes, prev, pager, next"
                        background
                        @current-change="loadLogs"
                        @size-change="loadLogs"
                    />
                </template>
            </TableCard>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
    createJob,
    deleteJob,
    pageJobLogs,
    pageJobs,
    pauseJob,
    resumeJob,
    runJob,
    updateJob,
    type JobForm,
    type JobLogRow,
    type JobRow,
} from "@/api/system/job";
import ButtonMore, {
    type ButtonMoreItem,
} from "@/components/core/tables/button-more/index.vue";
import ButtonTable from "@/components/core/tables/button-table/index.vue";
import SearchCard from "@/components/core/tables/search-card/index.vue";
import Table from "@/components/core/tables/table/index.vue";
import TableCard from "@/components/core/tables/table-card/index.vue";
import { formatTime } from "@/utils/format";
import { useFormDraft } from "@/composables/useFormDraft";
import CronExpressionBuilder from "@/components/business/cron-expression-builder/index.vue";

defineOptions({ name: "SystemJob" });

const showSearch = ref(false);
const loading = ref(false);
const saving = ref(false);
const rows = ref<JobRow[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);
const logVisible = ref(false);
const logLoading = ref(false);
const logs = ref<JobLogRow[]>([]);
const logTotal = ref(0);
const logQuery = reactive({ pageNum: 1, pageSize: 10, jobName: "" });

const moreItems: ButtonMoreItem[] = [
    { key: "run", label: "执行一次", icon: "ri:play-line", auth: "sys:job:run" },
    { key: "log", label: "调度日志", icon: "ri:file-list-3-line", auth: "sys:job:list" },
];

const query = reactive({
    pageNum: 1,
    pageSize: 10,
    jobName: "",
    status: undefined as number | undefined,
});
const form = reactive<JobForm>({
    jobName: "",
    jobGroup: "SYSTEM",
    cronExpression: "",
    handlerName: "demoJob",
    params: "",
    misfirePolicy: 1,
    status: 0,
});

const draft = useFormDraft({
    key: "job-form",
    form,
    visible: dialogVisible,
    context: { editingId },
});

async function load() {
    loading.value = true;
    try {
        const result = await pageJobs({
            pageNum: query.pageNum,
            pageSize: query.pageSize,
            jobName: query.jobName || undefined,
            status: query.status,
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

function reset() {
    query.jobName = "";
    query.status = undefined;
    search();
}

function resetForm() {
    Object.assign(form, {
        jobName: "",
        jobGroup: "SYSTEM",
        cronExpression: "",
        handlerName: "demoJob",
        params: "",
        misfirePolicy: 1,
        status: 0,
    } satisfies JobForm);
}

function openCreate() {
    editingId.value = null;
    resetForm();
    dialogVisible.value = true;
}

function openEdit(row: JobRow) {
    editingId.value = row.id;
    Object.assign(form, {
        jobName: row.jobName,
        jobGroup: row.jobGroup,
        cronExpression: row.cronExpression,
        handlerName: row.handlerName,
        params: row.params ?? "",
        misfirePolicy: row.misfirePolicy,
        status: row.status,
    } satisfies JobForm);
    dialogVisible.value = true;
}

async function save() {
    if (!form.jobName || !form.cronExpression || !form.handlerName) {
        ElMessage.warning("任务名称、cron 表达式和调用处理器不能为空");
        return;
    }
    saving.value = true;
    try {
        if (editingId.value) {
            await updateJob(editingId.value, { ...form });
        } else {
            await createJob({ ...form });
        }
        ElMessage.success("保存成功");
        draft.clear();
        dialogVisible.value = false;
        load();
    } finally {
        saving.value = false;
    }
}

async function remove(row: JobRow) {
    await ElMessageBox.confirm(`确定删除任务「${row.jobName}」吗？`, "提示", {
        type: "warning",
    });
    await deleteJob(row.id);
    ElMessage.success("删除成功");
    load();
}

async function toggleStatus(row: JobRow) {
    if (row.status === 1) {
        await pauseJob(row.id);
    } else {
        await resumeJob(row.id);
    }
    load();
}

function handleMore(command: string | number, row: JobRow) {
    if (command === "run") {
        runOnce(row);
    } else if (command === "log") {
        openLogs(row);
    }
}

async function runOnce(row: JobRow) {
    await runJob(row.id);
    ElMessage.success("已触发执行");
}

function openLogs(row: JobRow) {
    logQuery.jobName = row.jobName;
    logQuery.pageNum = 1;
    logVisible.value = true;
    loadLogs();
}

async function loadLogs() {
    logLoading.value = true;
    try {
        const result = await pageJobLogs(logQuery);
        logs.value = result.records;
        logTotal.value = Number(result.total);
    } finally {
        logLoading.value = false;
    }
}

onMounted(load);
</script>
