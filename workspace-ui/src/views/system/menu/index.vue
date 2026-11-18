<template>
    <div class="system-page">
        <SearchCard v-show="showSearch">
            <div class="search-row">
                <div class="search-field">
                    <span class="search-label">菜单名称</span>
                    <el-input
                        v-model="filters.menuName"
                        placeholder="请输入菜单名称"
                        clearable
                        style="width: 180px"
                        @keyup.enter="search"
                    />
                </div>
                <div class="search-field">
                    <span class="search-label">状态</span>
                    <el-select
                        v-model="filters.status"
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
                    v-permission="'sys:menu:create'"
                    type="primary"
                    @click="openCreate"
                >
                    新增菜单
                </el-button>
                <el-button @click="toggleExpand">
                    {{ expandAll ? "收起" : "展开" }}
                </el-button>
            </template>

            <template #default="{ stripe, border, headerBackground, size }">
<Table
                :key="tableKey"
                v-loading="loading"
                :data="filteredMenus"
                row-key="id"
                :default-expand-all="expandAll"
                :tree-props="{ children: 'children' }"
            
                :stripe="stripe"
                :border="border"
                :class="[`table-size-${size}`, { 'no-header-bg': !headerBackground }]">
            <el-table-column prop="menuName" label="菜单名称" min-width="180" />
            <el-table-column label="类型" width="90">
                <template #default="{ row }">
                    <el-tag :type="typeTag(row.menuType)">
                        {{ typeLabel(row.menuType) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="path" label="路由" min-width="160" />
            <el-table-column prop="component" label="组件" min-width="180" />
            <el-table-column prop="permission" label="权限码" min-width="170" />
            <el-table-column prop="sortNo" label="排序" width="80" />
            <el-table-column label="状态" width="80">
                <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'info'">
                        {{ row.status === 1 ? "启用" : "停用" }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right" align="center" class-name="action-cell">
                <template #default="{ row }">
                    <ButtonTable
                        v-if="row.menuType !== 'BUTTON'"
                        v-permission="'sys:menu:create'"
                        type="add"
                        @click="openCreateChild(row)"
                    />
                    <ButtonTable
                        v-permission="'sys:menu:update'"
                        type="edit"
                        @click="openEdit(row)"
                    />
                    <ButtonTable
                        v-permission="'sys:menu:delete'"
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
            :title="editingId ? '编辑菜单' : '新增菜单'"
            width="620px"
        >
            <el-form :model="form" label-width="100px">
                <el-form-item label="上级菜单">
                    <el-tree-select
                        v-model="form.parentId"
                        :data="parentOptions"
                        :props="{
                            label: 'menuName',
                            value: 'id',
                            children: 'children',
                        }"
                        check-strictly
                        style="width: 100%"
                    />
                </el-form-item>
                <el-form-item label="菜单编码" required>
                    <el-input v-model.trim="form.menuCode" />
                </el-form-item>
                <el-form-item label="菜单名称" required>
                    <el-input v-model.trim="form.menuName" />
                </el-form-item>
                <el-form-item label="类型" required>
                    <el-radio-group v-model="form.menuType">
                        <el-radio value="DIR">目录</el-radio>
                        <el-radio value="MENU">菜单</el-radio>
                        <el-radio value="BUTTON">按钮</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item v-if="form.menuType !== 'BUTTON'" label="路由">
                    <el-input v-model.trim="form.path" placeholder="/system/xxx" />
                </el-form-item>
                <el-form-item v-if="form.menuType === 'MENU'" label="组件">
                    <el-input
                        v-model.trim="form.component"
                        placeholder="system/xxx/index"
                    />
                </el-form-item>
                <el-form-item v-if="form.menuType !== 'DIR'" label="权限码">
                    <el-input
                        v-model.trim="form.permission"
                        placeholder="sys:xxx:list"
                    />
                </el-form-item>
                <el-form-item label="图标">
                    <el-input v-model.trim="form.icon" />
                </el-form-item>
                <el-form-item label="排序">
                    <el-input-number v-model="form.sortNo" :min="0" />
                </el-form-item>
                <el-form-item label="显示">
                    <el-switch
                        v-model="form.hidden"
                        :active-value="1"
                        :inactive-value="0"
                    />
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
    createMenu,
    deleteMenu,
    getMenuTree,
    updateMenu,
    type MenuForm,
} from "@/api/system/menu";
import ButtonTable from "@/components/core/tables/button-table/index.vue";
import SearchCard from "@/components/core/tables/search-card/index.vue";
import Table from "@/components/core/tables/table/index.vue";
import TableCard from "@/components/core/tables/table-card/index.vue";
import type { MenuNode } from "@/types/system";
import { useFormDraft } from "@/composables/useFormDraft";

defineOptions({ name: "SystemMenu" });

interface ParentOption {
    id: string;
    menuName: string;
    children?: ParentOption[];
}

const showSearch = ref(false);
const loading = ref(false);
const saving = ref(false);
const menus = ref<MenuNode[]>([]);
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);
const expandAll = ref(false);
const tableKey = ref(0);
const filters = reactive({
    menuName: "",
    status: undefined as number | undefined,
});
const applied = reactive({
    menuName: "",
    status: undefined as number | undefined,
});

const form = reactive<MenuForm>({
    appCode: "platform",
    menuCode: "",
    parentId: "0",
    menuName: "",
    menuType: "MENU",
    path: "",
    component: "",
    permission: "",
    icon: "",
    sortNo: 0,
    hidden: 1,
    status: 1,
});

const draft = useFormDraft({
    key: "menu-form",
    form,
    visible: dialogVisible,
    context: { editingId },
});

const parentOptions = computed<ParentOption[]>(() => [
    { id: "0", menuName: "根目录", children: filterParent(menus.value) },
]);

const filteredMenus = computed(() =>
    filterTree(menus.value, applied.menuName, applied.status),
);

function filterParent(nodes: MenuNode[]): ParentOption[] {
    return nodes
        .filter((node) => node.menuType !== "BUTTON")
        .map((node) => ({
            id: node.id,
            menuName: node.menuName,
            children: node.children ? filterParent(node.children) : [],
        }));
}

function filterTree(
    nodes: MenuNode[],
    menuName: string,
    status?: number,
): MenuNode[] {
    const name = menuName.trim();
    const result: MenuNode[] = [];
    for (const node of nodes) {
        const children = filterTree(node.children ?? [], name, status);
        const match =
            (!name || node.menuName.includes(name)) &&
            (status === undefined || node.status === status);
        if (match || children.length > 0) {
            result.push({ ...node, children });
        }
    }
    return result;
}

function search() {
    applied.menuName = filters.menuName;
    applied.status = filters.status;
}

function reset() {
    filters.menuName = "";
    filters.status = undefined;
    search();
}

function toggleExpand() {
    expandAll.value = !expandAll.value;
    tableKey.value++;
}

function typeLabel(type: string) {
    return type === "DIR" ? "目录" : type === "MENU" ? "菜单" : "按钮";
}

function typeTag(type: string) {
    return type === "DIR" ? "warning" : type === "MENU" ? "success" : "info";
}

async function load() {
    loading.value = true;
    try {
        menus.value = await getMenuTree(true);
    } finally {
        loading.value = false;
    }
}

function resetForm() {
    Object.assign(form, {
        appCode: "platform",
        menuCode: "",
        parentId: "0",
        menuName: "",
        menuType: "MENU",
        path: "",
        component: "",
        permission: "",
        icon: "",
        sortNo: 0,
        hidden: 1,
        status: 1,
    } satisfies MenuForm);
}

function openCreate() {
    editingId.value = null;
    resetForm();
    dialogVisible.value = true;
}

function openCreateChild(row: MenuNode) {
    editingId.value = null;
    resetForm();
    form.parentId = row.id;
    form.menuType = row.menuType === "MENU" ? "BUTTON" : "MENU";
    dialogVisible.value = true;
}

function openEdit(row: MenuNode) {
    editingId.value = row.id;
    Object.assign(form, {
        appCode: "platform",
        menuCode: row.menuCode,
        parentId: row.parentId,
        menuName: row.menuName,
        menuType: row.menuType,
        path: row.path ?? "",
        component: row.component ?? "",
        permission: row.permission ?? "",
        icon: row.icon ?? "",
        sortNo: row.sortNo,
        hidden: row.hidden,
        status: row.status,
    } satisfies MenuForm);
    dialogVisible.value = true;
}

async function save() {
    if (!form.menuCode || !form.menuName) {
        ElMessage.warning("菜单编码和名称不能为空");
        return;
    }
    if (form.menuType === "MENU" && (!form.path || !form.component)) {
        ElMessage.warning("菜单类型必须填写路由和组件");
        return;
    }
    saving.value = true;
    try {
        if (editingId.value) {
            await updateMenu(editingId.value, { ...form });
        } else {
            await createMenu({ ...form });
        }
        ElMessage.success("保存成功");
        draft.clear();
        dialogVisible.value = false;
        load();
    } finally {
        saving.value = false;
    }
}

async function remove(row: MenuNode) {
    await ElMessageBox.confirm(`确定删除菜单「${row.menuName}」吗？`, "提示", {
        type: "warning",
    });
    await deleteMenu(row.id);
    ElMessage.success("删除成功");
    load();
}

onMounted(load);
</script>
