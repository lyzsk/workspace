<template>
    <div class="system-page">
        <TableCard @refresh="load">
            <template #actions>
                <el-button
                    v-permission="'sys:app:create'"
                    type="primary"
                    @click="openCreate"
                >
                    新增应用
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
                    <el-table-column prop="code" label="应用编码" min-width="120" />
                    <el-table-column prop="name" label="应用名称" min-width="180" />
                    <el-table-column prop="basePath" label="路由前缀" min-width="140" />
                    <el-table-column
                        prop="moduleEntry"
                        label="Qiankun HTML 入口"
                        min-width="230"
                        show-overflow-tooltip
                    />
                    <el-table-column
                        prop="styleEntry"
                        label="样式入口"
                        min-width="180"
                        show-overflow-tooltip
                    />
                    <el-table-column prop="sortNo" label="排序" width="80" />
                    <el-table-column label="状态" width="90">
                        <template #default="{ row }">
                            <el-tag :type="row.status === 1 ? 'success' : 'info'">
                                {{ row.status === 1 ? "启用" : "停用" }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column
                        label="操作"
                        width="90"
                        fixed="right"
                        align="center"
                        class-name="action-cell"
                    >
                        <template #default="{ row }">
                            <ButtonTable
                                v-permission="'sys:app:update'"
                                type="edit"
                                @click="openEdit(row)"
                            />
                            <ButtonTable
                                v-permission="'sys:app:delete'"
                                type="delete"
                                @click="remove(row)"
                            />
                        </template>
                    </el-table-column>
                </Table>
            </template>
        </TableCard>

        <el-dialog draggable
            v-model="dialogVisible"
            :title="editingId ? '编辑应用' : '新增应用'"
            width="620px"
        >
            <el-form :model="form" label-width="100px">
                <el-form-item label="应用编码" required>
                    <el-input
                        v-model.trim="form.code"
                        :disabled="!!editingId"
                        placeholder="如 ies"
                    />
                </el-form-item>
                <el-form-item label="应用名称" required>
                    <el-input v-model.trim="form.name" placeholder="如 IES（设备信息维护系统）" />
                </el-form-item>
                <el-form-item label="图标">
                    <el-input v-model.trim="form.icon" placeholder="如 ri:server-line" />
                </el-form-item>
                <el-form-item label="路由前缀" required>
                    <el-input v-model.trim="form.basePath" placeholder="如 /apps/ies" />
                </el-form-item>
                <el-form-item label="HTML 入口" required>
                    <el-input
                        v-model.trim="form.moduleEntry"
                        placeholder="开发如 http://localhost:3010/，生产如 /ies/"
                    />
                    <div class="form-help">
                        填写子应用入口页的 HTML 文档地址，须允许主应用跨域抓取；不得填写 JS 或 TS 模块地址。qiankun 3 仅接受 HTML 入口。
                    </div>
                </el-form-item>
                <el-form-item label="微前端样式入口">
                    <el-input
                        v-model.trim="form.styleEntry"
                        placeholder="如 /ies/micro/ies-micro.css"
                    />
                </el-form-item>
                <el-form-item label="访问权限码">
                    <el-input
                        :model-value="form.code ? `${form.code}:access` : ''"
                        disabled
                        placeholder="根据应用编码自动生成"
                    />
                </el-form-item>
                <el-form-item label="排序">
                    <el-input-number v-model="form.sortNo" :min="0" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch
                        v-model="form.status"
                        :active-value="1"
                        :inactive-value="0"
                    />
                </el-form-item>
                <el-form-item label="备注">
                    <el-input
                        v-model="form.remark"
                        type="textarea"
                        :rows="3"
                        maxlength="500"
                        show-word-limit
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="dialogVisible = false">取消</el-button>
                    <el-button type="primary" :loading="saving" @click="save">
                        保存
                    </el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
    createApp,
    deleteApp,
    listApps,
    updateApp,
    type AppForm,
    type AppInfo,
} from "@/api/system/app";
import ButtonTable from "@/components/core/tables/button-table/index.vue";
import Table from "@/components/core/tables/table/index.vue";
import TableCard from "@/components/core/tables/table-card/index.vue";
import { useFormDraft } from "@/composables/useFormDraft";
import { notifyDataChanged } from "@/utils/dataChange";

defineOptions({ name: "AppList" });

const loading = ref(false);
const saving = ref(false);
const rows = ref<AppInfo[]>([]);
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);

const form = ref<AppForm>({
    code: "",
    name: "",
    icon: "",
    basePath: "",
    moduleEntry: "",
    styleEntry: "",
    accessPermission: "",
    sortNo: 0,
    status: 1,
    remark: "",
});

const draft = useFormDraft({
    key: "app-form",
    form: form.value,
    visible: dialogVisible,
    context: { editingId },
});

async function load() {
    loading.value = true;
    try {
        rows.value = await listApps();
    } finally {
        loading.value = false;
    }
}

function resetForm() {
    Object.assign(form.value, {
        code: "",
        name: "",
        icon: "",
        basePath: "",
        moduleEntry: "",
        styleEntry: "",
    accessPermission: "",
        sortNo: 0,
        status: 1,
        remark: "",
    } satisfies AppForm);
}

function openCreate() {
    editingId.value = null;
    resetForm();
    dialogVisible.value = true;
}

function openEdit(row: AppInfo) {
    editingId.value = row.id;
    Object.assign(form.value, {
        code: row.code,
        name: row.name,
        icon: row.icon ?? "",
        basePath: row.basePath,
        moduleEntry: row.moduleEntry,
        styleEntry: row.styleEntry ?? "",
        accessPermission: row.accessPermission,
        sortNo: row.sortNo,
        status: row.status,
        remark: row.remark ?? "",
    } satisfies AppForm);
    dialogVisible.value = true;
}

async function save() {
    if (
        !form.value.code ||
        !form.value.name ||
        !form.value.basePath ||
        !form.value.moduleEntry
    ) {
        ElMessage.warning("应用编码、名称、路由和 HTML 入口不能为空");
        return;
    }
    saving.value = true;
    try {
        if (editingId.value) {
            await updateApp(editingId.value, { ...form.value });
        } else {
            await createApp({ ...form.value });
        }
        ElMessage.success("保存成功");
        draft.clear();
        dialogVisible.value = false;
        await load();
        notifyDataChanged("/api/system/apps");
    } finally {
        saving.value = false;
    }
}

async function remove(row: AppInfo) {
    await ElMessageBox.confirm(`确定删除应用「${row.name}」吗？`, "提示", {
        type: "warning",
    });
    await deleteApp(row.id);
    ElMessage.success("删除成功");
    await load();
    notifyDataChanged("/api/system/apps");
}

onMounted(load);
</script>

<style scoped>
.form-help {
    margin-top: 4px;
    color: var(--text-muted);
    font-size: 12px;
    line-height: 1.5;
}
</style>
