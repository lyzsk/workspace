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
                        <el-option label="启用" :value="1" />
                        <el-option label="停用" :value="0" />
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
                    v-permission="'sys:user:create'"
                    type="primary"
                    @click="openCreate"
                >
                    新增用户
                </el-button>
                <el-button
                    v-permission="'sys:user:delete'"
                    type="danger"
                    plain
                    :disabled="!selectedIds.length"
                    @click="batchRemove"
                >
                    批量删除
                </el-button>
                <el-button v-permission="'sys:user:list'" @click="exportData">
                    导出 Excel
                </el-button>
            </template>

            <template #default="{ stripe, border, headerBackground, size }">
<Table
                v-loading="loading"
                :data="rows"
               
                @selection-change="onSelectionChange"
                @expand-change="onExpandChange"
                @sort-change="onSortChange"
            
                :stripe="stripe"
                :border="border"
                :class="[`table-size-${size}`, { 'no-header-bg': !headerBackground }]">
            <el-table-column v-if="userStore.hasPerm('sys:user:app-grant')" type="expand" width="36">
                <template #default="{ row }">
                    <AppGrantPanel
                        v-if="expandedUserIds.includes(row.id)"
                        :user-id="row.id"
                        :dept-options="deptOptions"
                    />
                </template>
            </el-table-column>
            <el-table-column
                type="selection"
                width="44"
                :selectable="canSelect"
            />
            <el-table-column prop="username" label="用户名" min-width="120" sortable="custom" />
            <el-table-column prop="realName" label="姓名" min-width="100" />
            <el-table-column label="性别" width="80">
                <template #default="{ row }">{{ genderLabel(row.gender) }}</template>
            </el-table-column>
            <el-table-column prop="email" label="邮箱" min-width="160" />
            <el-table-column prop="mobile" label="手机号" min-width="130" />
            <el-table-column label="状态" width="80">
                <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'info'">
                        {{ row.status === 1 ? "启用" : "停用" }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="创建时间" width="170" prop="createTime" sortable="custom">
                <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right" align="center" class-name="action-cell">
                <template #default="{ row }">
                    <template v-if="row.username !== 'admin'">
                    <ButtonTable
                        v-permission="'sys:user:update'"
                        type="edit"
                        @click="openEdit(row)"
                    />
                    <ButtonTable
                        v-permission="'sys:user:delete'"
                        type="delete"
                        @click="remove(row)"
                    />
                    </template>
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

        <el-dialog draggable
            v-model="dialogVisible"
            :title="editingId ? '编辑用户' : '新增用户'"
            width="620px"
        >
            <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model.trim="form.username" />
                </el-form-item>
                <el-form-item label="密码" prop="password">
                    <el-input
                        v-model.trim="form.password"
                        type="password"
                        show-password
                        :placeholder="editingId ? '留空则不修改' : '请输入密码'"
                    />
                </el-form-item>
                <el-form-item label="姓名" prop="realName">
                    <el-input v-model.trim="form.realName" />
                </el-form-item>
                <el-form-item label="性别">
                    <el-radio-group v-model="form.gender">
                        <el-radio :value="0">未知</el-radio>
                        <el-radio :value="1">男</el-radio>
                        <el-radio :value="2">女</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="部门">
                    <el-tree-select
                        v-model="form.deptId"
                        :data="deptOptions"
                        :props="{
                            label: 'deptName',
                            value: 'id',
                            children: 'children',
                        }"
                        check-strictly
                        clearable
                        style="width: 100%"
                        placeholder="请选择部门"
                    />
                </el-form-item>
                <el-form-item v-permission="'sys:user:assign'" label="角色">
                    <el-select
                        v-model="form.roleIds"
                        multiple
                        clearable
                        style="width: 100%"
                        placeholder="请选择角色"
                    >
                        <el-option
                            v-for="role in roleOptions"
                            :key="role.id"
                            :label="role.roleName"
                            :value="role.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="邮箱">
                    <el-input v-model.trim="form.email" />
                </el-form-item>
                <el-form-item label="手机号">
                    <el-input v-model.trim="form.mobile" />
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import {
    createUser,
    deleteUser,
    deleteUsers,
    exportUsers,
    pageUsers,
    updateUser,
    type UserForm,
    type UserRow,
} from "@/api/system/user";
import { getMe } from "@/api/auth";
import { useUserStore } from "@/store/modules/user";
import ButtonTable from "@/components/core/tables/button-table/index.vue";
import AppGrantPanel from "./components/AppGrantPanel.vue";
import SearchCard from "@/components/core/tables/search-card/index.vue";
import Table from "@/components/core/tables/table/index.vue";
import TableCard from "@/components/core/tables/table-card/index.vue";
import { getDeptList } from "@/api/system/dept";
import { listRoles } from "@/api/system/role";
import type { DeptNode, RoleRow } from "@/types/system";
import { formatTime } from "@/utils/format";
import { useFormDraft } from "@/composables/useFormDraft";

defineOptions({ name: "SystemUser" });

const showSearch = ref(false);
const userStore = useUserStore();
const loading = ref(false);
const saving = ref(false);
const rows = ref<UserRow[]>([]);
const expandedUserIds = ref<string[]>([]);
const total = ref(0);
const selectedRows = ref<UserRow[]>([]);
const selectedIds = computed(() => selectedRows.value.map((row) => row.id));
const deptOptions = ref<DeptNode[]>([]);
const roleOptions = ref<RoleRow[]>([]);
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);
const formRef = ref<FormInstance>();

const query = reactive({
    pageNum: 1,
    pageSize: 10,
    username: "",
    status: undefined as number | undefined,
    sortBy: undefined as string | undefined,
    order: undefined as string | undefined,
});

const form = reactive<UserForm>({
    username: "",
    password: "",
    realName: "",
    gender: 0,
    email: "",
    mobile: "",
    deptId: undefined,
    status: 1,
    roleIds: [],
});

const rules: FormRules = {
    username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
    realName: [{ required: true, message: "请输入姓名", trigger: "blur" }],
};

const draft = useFormDraft({
    key: "user-form",
    form,
    visible: dialogVisible,
    context: { editingId },
});

function canSelect(row: UserRow) {
    return row.username !== "admin";
}

function genderLabel(gender: number) {
    return gender === 1 ? "男" : gender === 2 ? "女" : "未知";
}

async function load() {
    loading.value = true;
    try {
        const result = await pageUsers({
            pageNum: query.pageNum,
            pageSize: query.pageSize,
            username: query.username || undefined,
            status: query.status,
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
    query.status = undefined;
    query.sortBy = undefined;
    query.order = undefined;
    query.pageNum = 1;
    load();
}

function resetForm() {
    Object.assign(form, {
        username: "",
        password: "",
        realName: "",
        gender: 0,
        email: "",
        mobile: "",
        deptId: undefined,
        status: 1,
        roleIds: [],
    } satisfies UserForm);
}

function openCreate() {
    editingId.value = null;
    resetForm();
    dialogVisible.value = true;
}

function openEdit(row: UserRow) {
    editingId.value = row.id;
    Object.assign(form, {
        username: row.username,
        password: "",
        realName: row.realName,
        gender: row.gender,
        email: row.email ?? "",
        mobile: row.mobile ?? "",
        deptId: row.deptId,
        status: row.status,
        roleIds: row.roleIds ?? [],
    } satisfies UserForm);
    dialogVisible.value = true;
}

async function save() {
    const valid = await formRef.value?.validate().catch(() => false);
    if (!valid) {
        return;
    }
    if (!editingId.value && !form.password) {
        ElMessage.warning("新增用户必须填写密码");
        return;
    }
    saving.value = true;
    try {
        if (editingId.value) {
            await updateUser(editingId.value, { ...form });
        } else {
            await createUser({ ...form });
        }
        ElMessage.success("保存成功");
        draft.clear();
        dialogVisible.value = false;
        load();
        if (editingId.value && editingId.value === userStore.user?.id) {
            userStore.setUser(await getMe());
        }
    } finally {
        saving.value = false;
    }
}

async function remove(row: UserRow) {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, "提示", {
        type: "warning",
    });
    await deleteUser(row.id);
    ElMessage.success("删除成功");
    load();
}

function onSelectionChange(selection: UserRow[]) {
    selectedRows.value = selection;
}

function onExpandChange(_row: UserRow, expandedRows: UserRow[]) {
    expandedUserIds.value = expandedRows.map(item => item.id);
}

async function batchRemove() {
    if (!selectedIds.value.length) {
        return;
    }
    await ElMessageBox.confirm(
        `确定删除选中的 ${selectedIds.value.length} 个用户吗？`,
        "提示",
        { type: "warning" },
    );
    await deleteUsers(selectedIds.value);
    ElMessage.success("批量删除成功");
    selectedRows.value = [];
    load();
}

async function exportData() {
    await exportUsers({
        username: query.username || undefined,
        status: query.status,
    });
}

onMounted(async () => {
    load();
    deptOptions.value = await getDeptList();
    const roles = await listRoles();
    roleOptions.value = userStore.user?.roles?.includes("SUPER_ADMIN")
        ? roles
        : roles.filter((role) => role.roleCode !== "SUPER_ADMIN");
});
</script>

<style scoped>
:deep(.el-table__expanded-cell) {
    padding: 0 !important;
}
</style>
