<template>
    <div class="system-page">
        <SearchCard v-show="showSearch">
            <div class="search-row">
                <div class="search-field">
                    <span class="search-label">角色编码</span>
                    <el-input
                        v-model="query.roleCode"
                        placeholder="请输入角色编码"
                        clearable
                        style="width: 160px"
                        @keyup.enter="search"
                    />
                </div>
                <div class="search-field">
                    <span class="search-label">角色名称</span>
                    <el-input
                        v-model="query.roleName"
                        placeholder="请输入角色名称"
                        clearable
                        style="width: 160px"
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

        <div class="app-scope-selector">
            <span>所属应用</span>
            <el-select v-model="query.appCode" style="width: 240px" @change="appChanged">
                <el-option
                    v-for="app in appOptions"
                    :key="app.code"
                    :label="app.name"
                    :value="app.code"
                />
            </el-select>
        </div>

        <TableCard searchable v-model:show-search="showSearch" @refresh="load">
            <template #actions>
                <el-button
                    v-permission="'sys:role:create'"
                    type="primary"
                    @click="openCreate"
                >
                    新增角色
                </el-button>
                <el-button v-permission="'sys:role:list'" @click="exportData">
                    导出 Excel
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
                    @sort-change="onSortChange"
                >
                    <el-table-column prop="roleName" label="角色名称" min-width="140" />
                    <el-table-column prop="roleCode" label="权限字符" min-width="140" />
                    <el-table-column label="数据范围" width="120">
                        <template #default="{ row }">
                            {{ dataScopeLabel(row.dataScope) }}
                        </template>
                    </el-table-column>
                    <el-table-column
                        prop="roleSort"
                        label="显示顺序"
                        width="100"
                        sortable="custom"
                    />
                    <el-table-column label="状态" width="80">
                        <template #default="{ row }">
                            <el-tag :type="row.status === 1 ? 'success' : 'info'">
                                {{ row.status === 1 ? "启用" : "停用" }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="remark" label="备注" min-width="160" />
                    <el-table-column
                        label="创建时间"
                        width="170"
                        prop="createTime"
                        sortable="custom"
                    >
                        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
                    </el-table-column>
                    <el-table-column
                        label="操作"
                        width="90"
                        fixed="right"
                        align="center"
                        class-name="action-cell"
                    >
                        <template #default="{ row }">
                            <ButtonMore
                                v-if="row.roleCode !== 'SUPER_ADMIN'"
                                :list="moreItems"
                                @click="(item) => handleRoleCommand(item.key, row)"
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

        <el-dialog draggable
            v-model="dialogVisible"
            :title="editingId ? '编辑角色' : '新增角色'"
            width="720px"
        >
            <el-alert
                v-if="isSuperAdminRole"
                title="超级管理员角色受保护，不允许修改"
                type="warning"
                :closable="false"
                show-icon
                style="margin-bottom: 16px"
            />
            <el-form :model="form" :disabled="isSuperAdminRole" label-width="100px">
                <el-form-item label="角色名称" required>
                    <el-input v-model.trim="form.roleName" />
                </el-form-item>
                <el-form-item label="权限字符" required>
                    <el-input v-model.trim="form.roleCode" />
                </el-form-item>
                <el-form-item label="显示顺序" required>
                    <el-input-number v-model="form.roleSort" :min="0" />
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
                <el-form-item label="数据范围">
                    <el-select v-model="form.dataScope" style="width: 100%">
                        <el-option
                            v-for="item in dataScopes"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item v-if="form.dataScope === 2" label="指定部门">
                    <el-tree-select
                        v-model="form.deptIds"
                        :data="deptOptions"
                        :props="{
                            label: 'deptName',
                            value: 'id',
                            children: 'children',
                        }"
                        multiple
                        show-checkbox
                        check-strictly
                        clearable
                        style="width: 100%"
                        placeholder="请选择部门"
                    />
                </el-form-item>
                <el-form-item label="菜单权限">
                    <div class="permission-panel">
                        <div class="permission-toolbar">
                            <el-button @click="toggleExpandAll">
                                {{ expandAll ? "折叠" : "展开" }}
                            </el-button>
                            <el-button @click="toggleSelectAll">
                                {{ isSelectAll ? "全不选" : "全选" }}
                            </el-button>
                            <el-checkbox v-model="form.menuCheckStrictly">
                                父子联动
                            </el-checkbox>
                        </div>
                        <el-tree
                            ref="menuTreeRef"
                            :data="menuOptions"
                            show-checkbox
                            :check-strictly="!form.menuCheckStrictly"
                            node-key="id"
                            :props="treeProps"
                            class="permission-tree"
                            @check="onMenuCheck"
                        />
                    </div>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="dialogVisible = false">取消</el-button>
                    <el-button
                        type="primary"
                        :loading="saving"
                        :disabled="isSuperAdminRole || menuLoading"
                        @click="save"
                    >
                        保存
                    </el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
    createRole,
    deleteRole,
    exportRoles,
    getRoleMenuIds,
    pageRoles,
    updateRole,
    type RoleForm,
} from "@/api/system/role";
import { getMenuTree } from "@/api/system/menu";
import { getDeptList } from "@/api/system/dept";
import { listApps, type AppInfo } from "@/api/system/app";
import ButtonMore, {
    type ButtonMoreItem,
} from "@/components/core/tables/button-more/index.vue";
import SearchCard from "@/components/core/tables/search-card/index.vue";
import Table from "@/components/core/tables/table/index.vue";
import TableCard from "@/components/core/tables/table-card/index.vue";
import type { DeptNode, MenuNode, RoleRow } from "@/types/system";
import { formatTime } from "@/utils/format";
import { useFormDraft } from "@/composables/useFormDraft";

defineOptions({ name: "SystemRole" });

interface TreeNodeState {
    expanded: boolean;
}

interface TreeRef {
    getCheckedKeys: (leafOnly?: boolean) => string[];
    getHalfCheckedKeys: () => string[];
    setCheckedKeys: (keys: string[], leafOnly?: boolean) => void;
    store: {
        nodesMap: Record<string, TreeNodeState>;
    };
}

const showSearch = ref(false);
const loading = ref(false);
const saving = ref(false);
const menuLoading = ref(false);
const rows = ref<RoleRow[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);
const menuOptions = ref<MenuNode[]>([]);
const deptOptions = ref<DeptNode[]>([]);
const apps = ref<AppInfo[]>([]);
const appOptions = computed(() => [{ code: "workspace", name: "工作台" }, ...apps.value]);
const menuTreeRef = ref<TreeRef>();
const expandAll = ref(false);
const isSelectAll = ref(false);
let roleEditVersion = 0;
let roleListVersion = 0;

const isSuperAdminRole = computed(
    () => editingId.value !== null && form.roleCode === "SUPER_ADMIN",
);
const treeProps = computed(() => ({
    label: "menuName",
    children: "children",
    disabled: () => isSuperAdminRole.value,
}));

const dataScopes = [
    { value: 1, label: "全部数据" },
    { value: 2, label: "指定部门" },
    { value: 3, label: "本部门" },
    { value: 4, label: "本部门及以下" },
    { value: 5, label: "仅本人" },
];

const query = reactive({
    pageNum: 1,
    pageSize: 10,
    appCode: "workspace",
    roleCode: "",
    roleName: "",
    status: undefined as number | undefined,
    sortBy: undefined as string | undefined,
    order: undefined as string | undefined,
});

const form = reactive<RoleForm>({
    appCode: "workspace",
    roleCode: "",
    roleName: "",
    dataScope: 5,
    roleSort: 0,
    menuCheckStrictly: true,
    status: 1,
    remark: "",
    menuIds: [],
    deptIds: [],
});

const draft = useFormDraft({
    key: "role-form",
    form,
    visible: dialogVisible,
    context: { editingId },
});

const moreItems: ButtonMoreItem[] = [
    {
        key: "edit",
        label: "编辑角色",
        icon: "ri:edit-2-line",
        auth: "sys:role:update",
    },
    {
        key: "delete",
        label: "删除角色",
        icon: "ri:delete-bin-4-line",
        color: "var(--art-error)",
        auth: "sys:role:delete",
    },
];

function dataScopeLabel(value: number) {
    return dataScopes.find((item) => item.value === value)?.label ?? value;
}

function handleRoleCommand(command: string | number, row: RoleRow) {
    if (command === "edit") {
        openEdit(row);
    } else if (command === "delete") {
        remove(row);
    }
}

async function load() {
    const version = ++roleListVersion;
    const appCode = query.appCode;
    loading.value = true;
    try {
        const result = await pageRoles({
            pageNum: query.pageNum,
            pageSize: query.pageSize,
            appCode,
            roleCode: query.roleCode || undefined,
            roleName: query.roleName || undefined,
            status: query.status,
            sortBy: query.sortBy,
            order: query.order,
        });
        if (version === roleListVersion) {
            rows.value = result.records;
            total.value = Number(result.total);
        }
    } finally {
        if (version === roleListVersion) {
            loading.value = false;
        }
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

async function exportData() {
    await exportRoles({
        appCode: query.appCode,
        roleCode: query.roleCode || undefined,
        roleName: query.roleName || undefined,
        status: query.status,
        sortBy: query.sortBy,
        order: query.order,
    });
}

function appChanged() {
    roleEditVersion += 1;
    query.pageNum = 1;
    dialogVisible.value = false;
    menuOptions.value = [];
    load();
}

function reset() {
    query.roleCode = "";
    query.roleName = "";
    query.status = undefined;
    query.sortBy = undefined;
    query.order = undefined;
    query.pageNum = 1;
    load();
}

function resetForm() {
    Object.assign(form, {
        appCode: query.appCode,
        roleCode: "",
        roleName: "",
        dataScope: 5,
        roleSort: 0,
        menuCheckStrictly: true,
        status: 1,
        remark: "",
        menuIds: [],
        deptIds: [],
    } satisfies RoleForm);
    isSelectAll.value = false;
}

async function openCreate() {
    const version = ++roleEditVersion;
    editingId.value = null;
    resetForm();
    menuOptions.value = [];
    menuLoading.value = true;
    dialogVisible.value = true;
    try {
        const menus = await getMenuTree(true, query.appCode);
        if (version !== roleEditVersion) {
            return;
        }
        menuOptions.value = menus;
        await nextTick();
        menuTreeRef.value?.setCheckedKeys([], false);
    } finally {
        if (version === roleEditVersion) {
            menuLoading.value = false;
        }
    }
}

async function openEdit(row: RoleRow) {
    const version = ++roleEditVersion;
    const appCode = row.appCode ?? query.appCode;
    editingId.value = row.id;
    Object.assign(form, {
        appCode,
        roleCode: row.roleCode,
        roleName: row.roleName,
        dataScope: row.dataScope,
        roleSort: row.roleSort,
        menuCheckStrictly: row.menuCheckStrictly !== false,
        status: row.status,
        remark: row.remark ?? "",
        menuIds: [],
        deptIds: row.deptIds ?? [],
    } satisfies RoleForm);
    menuOptions.value = [];
    menuLoading.value = true;
    dialogVisible.value = true;
    try {
        const [menus, menuIds] = await Promise.all([
            getMenuTree(true, appCode),
            getRoleMenuIds(row.id),
        ]);
        if (version !== roleEditVersion || editingId.value !== row.id) {
            return;
        }
        menuOptions.value = menus;
        form.menuIds = menuIds;
        await nextTick();
        menuTreeRef.value?.setCheckedKeys(menuIds, false);
    } finally {
        if (version === roleEditVersion) {
            menuLoading.value = false;
        }
    }
}

function collectWithChildren(node: MenuNode, ids: string[] = []) {
    ids.push(node.id);
    for (const child of node.children ?? []) {
        collectWithChildren(child, ids);
    }
    return ids;
}

function allMenuKeys() {
    return menuOptions.value.flatMap((item) => collectWithChildren(item));
}

function syncMenuIds() {
    const checked = menuTreeRef.value?.getCheckedKeys(false) ?? [];
    const halfChecked = menuTreeRef.value?.getHalfCheckedKeys() ?? [];
    form.menuIds = [...new Set([...checked, ...halfChecked])];
}

function onMenuCheck(node: MenuNode, state: { checkedKeys: string[] }) {
    if (isSuperAdminRole.value) {
        return;
    }
    if (form.menuCheckStrictly) {
        syncMenuIds();
        isSelectAll.value =
            state.checkedKeys.length === allMenuKeys().length && allMenuKeys().length > 0;
        return;
    }
    const checked = new Set(state.checkedKeys);
    const affected = collectWithChildren(node);
    if (checked.has(node.id)) {
        affected.forEach((id) => checked.add(id));
        const nodes = new Map<string, MenuNode>();
        const register = (items: MenuNode[]) => {
            for (const item of items) {
                nodes.set(item.id, item);
                register(item.children ?? []);
            }
        };
        register(menuOptions.value);
        let parentId = node.parentId;
        while (parentId && parentId !== "0") {
            checked.add(parentId);
            parentId = nodes.get(parentId)?.parentId ?? "0";
        }
    } else {
        affected.forEach((id) => checked.delete(id));
    }
    menuTreeRef.value?.setCheckedKeys([...checked], false);
    syncMenuIds();
    isSelectAll.value = checked.size === allMenuKeys().length && checked.size > 0;
}

function toggleExpandAll() {
    const tree = menuTreeRef.value;
    if (!tree) {
        return;
    }
    Object.values(tree.store.nodesMap).forEach((node) => {
        node.expanded = !expandAll.value;
    });
    expandAll.value = !expandAll.value;
}

function toggleSelectAll() {
    if (isSuperAdminRole.value) {
        return;
    }
    const keys = isSelectAll.value ? [] : allMenuKeys();
    menuTreeRef.value?.setCheckedKeys(keys, false);
    syncMenuIds();
    isSelectAll.value = !isSelectAll.value;
}

async function save() {
    if (!form.roleCode || !form.roleName) {
        ElMessage.warning("角色名称和权限字符不能为空");
        return;
    }
    syncMenuIds();
    saving.value = true;
    try {
        if (editingId.value) {
            await updateRole(editingId.value, { ...form });
        } else {
            await createRole({ ...form });
        }
        ElMessage.success("保存成功");
        draft.clear();
        dialogVisible.value = false;
        load();
    } finally {
        saving.value = false;
    }
}

async function remove(row: RoleRow) {
    await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, "提示", {
        type: "warning",
    });
    await deleteRole(row.id);
    ElMessage.success("删除成功");
    load();
}

watch(dialogVisible, async (visible) => {
    if (visible) {
        await nextTick();
        menuTreeRef.value?.setCheckedKeys(form.menuIds ?? [], false);
    } else {
        roleEditVersion += 1;
        menuLoading.value = false;
    }
});

onMounted(async () => {
    await Promise.all([
        load(),
        getDeptList().then((items) => (deptOptions.value = items)),
        listApps().then((items) => (apps.value = items)),
    ]);
});
</script>

<style scoped>
.app-scope-selector {
    display: flex;
    gap: 12px;
    align-items: center;
    margin-bottom: 12px;
}

.permission-panel {
    width: 100%;
    border: 1px solid var(--border);
    border-radius: var(--custom-radius);
}

.permission-toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    border-bottom: 1px solid var(--border);
}

.permission-tree {
    max-height: 42vh;
    padding: 8px;
    overflow: auto;
}
</style>
