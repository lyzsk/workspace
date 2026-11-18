<template>
    <div class="system-page">
        <TableCard @refresh="load">
            <template #actions>
                <el-button
                    v-permission="'sys:dept:create'"
                    type="primary"
                    @click="openCreate()"
                >
                    新增部门
                </el-button>
            </template>

            <template #default="{ stripe, border, headerBackground, size }">
                <Table
                    v-loading="loading"
                    :data="depts"
                    row-key="id"
                    :tree-props="{ children: 'children' }"
                    :stripe="stripe"
                    :border="border"
                    :class="[`table-size-${size}`, { 'no-header-bg': !headerBackground }]"
                >
                    <el-table-column prop="deptName" label="部门名称" min-width="240" />
                    <el-table-column prop="sortNo" label="排序" width="90" />
                    <el-table-column label="状态" width="90">
                        <template #default="{ row }">
                            <el-tag :type="row.status === 1 ? 'success' : 'info'">
                                {{ row.status === 1 ? "正常" : "停用" }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="创建时间" width="180">
                        <template #default="{ row }">
                            {{ formatTime(row.createTime) }}
                        </template>
                    </el-table-column>
                    <el-table-column
                        label="操作"
                        width="140"
                        fixed="right"
                        align="center"
                        class-name="action-cell"
                    >
                        <template #default="{ row }">
                            <ButtonTable
                                v-permission="'sys:dept:create'"
                                type="add"
                                @click="openCreate(row)"
                            />
                            <ButtonTable
                                v-if="row.id !== '1'"
                                v-permission="'sys:dept:update'"
                                type="edit"
                                @click="openEdit(row)"
                            />
                            <ButtonTable
                                v-if="row.id !== '1'"
                                v-permission="'sys:dept:delete'"
                                type="delete"
                                @click="remove(row)"
                            />
                        </template>
                    </el-table-column>
                </Table>
            </template>
        </TableCard>

        <el-dialog
            v-model="dialogVisible"
            :title="editingId ? '编辑部门' : '新增部门'"
            width="620px"
        >
            <el-form :model="form" label-width="100px">
                <el-form-item label="上级部门">
                    <el-tree-select
                        v-model="form.parentId"
                        :data="parentOptions"
                        :props="{
                            label: 'deptName',
                            value: 'id',
                            children: 'children',
                        }"
                        check-strictly
                        style="width: 100%"
                    />
                </el-form-item>
                <el-form-item label="部门编码" required>
                    <el-input v-model.trim="form.deptCode" />
                </el-form-item>
                <el-form-item label="部门名称" required>
                    <el-input v-model.trim="form.deptName" />
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
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
    createDept,
    deleteDept,
    getDeptTree,
    updateDept,
    type DeptForm,
} from "@/api/system/dept";
import ButtonTable from "@/components/core/tables/button-table/index.vue";
import Table from "@/components/core/tables/table/index.vue";
import TableCard from "@/components/core/tables/table-card/index.vue";
import { formatTime } from "@/utils/format";
import type { DeptNode } from "@/types/system";
import { useFormDraft } from "@/composables/useFormDraft";

defineOptions({ name: "SystemDept" });

interface ParentOption {
    id: string;
    deptName: string;
    children?: ParentOption[];
}

const loading = ref(false);
const saving = ref(false);
const depts = ref<DeptNode[]>([]);
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);

const form = reactive<DeptForm>({
    deptCode: "",
    deptName: "",
    parentId: "0",
    sortNo: 0,
    status: 1,
});

const draft = useFormDraft({
    key: "dept-form",
    form,
    visible: dialogVisible,
    context: { editingId },
});

const parentOptions = computed<ParentOption[]>(() => [
    { id: "0", deptName: "根部门", children: filterParent(depts.value) },
]);

function filterParent(nodes: DeptNode[]): ParentOption[] {
    return nodes.map((node) => ({
        id: node.id,
        deptName: node.deptName,
        children: node.children ? filterParent(node.children) : [],
    }));
}

async function load() {
    loading.value = true;
    try {
        depts.value = await getDeptTree();
    } finally {
        loading.value = false;
    }
}

function resetForm() {
    Object.assign(form, {
        deptCode: "",
        deptName: "",
        parentId: "0",
        sortNo: 0,
        status: 1,
    } satisfies DeptForm);
}

function openCreate(parent?: DeptNode) {
    editingId.value = null;
    resetForm();
    form.parentId = parent?.id ?? "0";
    dialogVisible.value = true;
}

function openEdit(row: DeptNode) {
    editingId.value = row.id;
    Object.assign(form, {
        deptCode: row.deptCode,
        deptName: row.deptName,
        parentId: row.parentId,
        sortNo: row.sortNo,
        status: row.status,
    } satisfies DeptForm);
    dialogVisible.value = true;
}

async function save() {
    if (!form.deptCode || !form.deptName) {
        ElMessage.warning("部门编码和名称不能为空");
        return;
    }
    saving.value = true;
    try {
        if (editingId.value) {
            await updateDept(editingId.value, { ...form });
        } else {
            await createDept({ ...form });
        }
        ElMessage.success("保存成功");
        draft.clear();
        dialogVisible.value = false;
        await load();
    } finally {
        saving.value = false;
    }
}

async function remove(row: DeptNode) {
    await ElMessageBox.confirm(`确定删除部门「${row.deptName}」吗？`, "提示", {
        type: "warning",
    });
    await deleteDept(row.id);
    ElMessage.success("删除成功");
    await load();
}

onMounted(load);
</script>
