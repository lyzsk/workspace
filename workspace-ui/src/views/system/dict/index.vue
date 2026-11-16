<template>
    <div class="system-page">
        <SearchCard v-show="showSearch">
            <div class="search-row">
                <div class="search-field">
                    <span class="search-label">字典名称</span>
                    <el-input
                        v-model="typeQuery.dictName"
                        placeholder="请输入字典名称"
                        clearable
                        style="width: 160px"
                        @keyup.enter="searchTypes"
                    />
                </div>
                <div class="search-field">
                    <span class="search-label">字典类型</span>
                    <el-input
                        v-model="typeQuery.dictType"
                        placeholder="请输入字典类型"
                        clearable
                        style="width: 180px"
                        @keyup.enter="searchTypes"
                    />
                </div>
                <div class="search-field">
                    <span class="search-label">状态</span>
                    <el-select
                        v-model="typeQuery.status"
                        placeholder="请选择状态"
                        clearable
                        style="width: 120px"
                    >
                        <el-option label="启用" :value="1" />
                        <el-option label="停用" :value="0" />
                    </el-select>
                </div>
                <div class="search-actions">
                    <el-button type="primary" @click="searchTypes">查询</el-button>
                    <el-button @click="resetTypes">重置</el-button>
                </div>
            </div>
        </SearchCard>

        <TableCard
                searchable
                v-model:show-search="showSearch"
                @refresh="loadTypes"
            >
                <template #actions>
                    <el-button
                        v-permission="'sys:dict:create'"
                        type="primary"
                        @click="openTypeCreate"
                    >
                        新增类型
                    </el-button>
                    <el-button v-permission="'sys:dict:list'" @click="exportTypes">
                        导出 Excel
                    </el-button>
                </template>
                <template #default="{ stripe, border, headerBackground, size }">
                    <Table
                        v-loading="typeLoading"
                        :data="types"
                        :stripe="stripe"
                        :border="border"
                        :class="[
                            `table-size-${size}`,
                            { 'no-header-bg': !headerBackground },
                        ]"
                        highlight-current-row
                    >
                        <el-table-column prop="id" label="字典编号" width="90" />
                        <el-table-column prop="dictName" label="字典名称" min-width="110" />
                        <el-table-column prop="dictType" label="字典类型" min-width="150">
                            <template #default="{ row }">
                                <el-button link type="primary" @click="openDataDialog(row)">
                                    {{ row.dictType }}
                                </el-button>
                            </template>
                        </el-table-column>
                        <el-table-column label="状态" width="80">
                            <template #default="{ row }">
                                <el-tag :type="row.status === 1 ? 'success' : 'info'">
                                    {{ row.status === 1 ? "启用" : "停用" }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="remark" label="备注" min-width="110" />
                        <el-table-column label="创建时间" width="160" prop="createTime" sortable>
                            <template #default="{ row }">
                                {{ formatTime(row.createTime) }}
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" width="90" align="center" class-name="action-cell">
                            <template #default="{ row }">
                                <ButtonTable
                                    v-permission="'sys:dict:update'"
                                    type="edit"
                                    @click="openTypeEdit(row)"
                                />
                                <ButtonTable
                                    v-permission="'sys:dict:delete'"
                                    type="delete"
                                    @click="removeType(row)"
                                />
                            </template>
                        </el-table-column>
                    </Table>
                </template>
            </TableCard>

        <el-dialog
            v-model="dataTableVisible"
            :title="`字典数据 - ${selectedType?.dictName || ''}`"
            width="1100px"
        >
            <TableCard @refresh="loadData">
                <template #actions>
                    <span class="current-type">
                        当前字典：{{ selectedType?.dictName || "未选择" }}
                    </span>
                    <el-button
                        v-permission="'sys:dict:list'"
                        :disabled="!selectedType"
                        @click="exportDataRows"
                    >
                        导出 Excel
                    </el-button>
                    <el-button
                        v-permission="'sys:dict:create'"
                        type="primary"
                        :disabled="!selectedType"
                        @click="openDataCreate"
                    >
                        新增数据
                    </el-button>
                </template>
                <template #default="{ stripe, border, headerBackground, size }">
                    <Table
                        v-loading="dataLoading"
                        :data="dataRows"
                        :stripe="stripe"
                        :border="border"
                        :class="[
                            `table-size-${size}`,
                            { 'no-header-bg': !headerBackground },
                        ]"
                    >
                        <el-table-column prop="id" label="字典编号" width="90" />
                        <el-table-column prop="label" label="显示文本" min-width="120" />
                        <el-table-column prop="value" label="字典值" min-width="100" />
                        <el-table-column prop="sortNo" label="排序" width="70" />
                        <el-table-column label="状态" width="80">
                            <template #default="{ row }">
                                <el-tag :type="row.status === 1 ? 'success' : 'info'">
                                    {{ row.status === 1 ? "启用" : "停用" }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="remark" label="备注" min-width="100" />
                        <el-table-column label="创建时间" width="160" prop="createTime" sortable>
                            <template #default="{ row }">
                                {{ formatTime(row.createTime) }}
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" width="90" align="center" class-name="action-cell">
                            <template #default="{ row }">
                                <ButtonTable
                                    v-permission="'sys:dict:update'"
                                    type="edit"
                                    @click="openDataEdit(row)"
                                />
                                <ButtonTable
                                    v-permission="'sys:dict:delete'"
                                    type="delete"
                                    @click="removeData(row)"
                                />
                            </template>
                        </el-table-column>
                    </Table>
                </template>
                <template #pagination>
                    <el-pagination
                        v-model:current-page="dataQuery.pageNum"
                        v-model:page-size="dataQuery.pageSize"
                        :total="Number(dataTotal)"
                        layout="total, prev, pager, next"
                        background
                        @current-change="loadData"
                    />
                </template>
            </TableCard>
        </el-dialog>

        <el-dialog
            v-model="typeDialogVisible"
            :title="editingTypeId ? '编辑字典类型' : '新增字典类型'"
            width="480px"
        >
            <el-form :model="typeForm" label-width="90px">
                <el-form-item label="字典类型" required>
                    <el-input v-model.trim="typeForm.dictType" placeholder="如 sys_user_status" />
                </el-form-item>
                <el-form-item label="字典名称" required>
                    <el-input v-model.trim="typeForm.dictName" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch v-model="typeForm.status" :active-value="1" :inactive-value="0" />
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model.trim="typeForm.remark" type="textarea" />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="typeDialogVisible = false">取消</el-button>
                    <el-button type="primary" :loading="saving" @click="saveType">保存</el-button>
                </div>
            </template>
        </el-dialog>

        <el-dialog
            v-model="dataDialogVisible"
            :title="editingDataId ? '编辑字典数据' : '新增字典数据'"
            width="480px"
        >
            <el-form :model="dataForm" label-width="90px">
                <el-form-item label="显示文本" required>
                    <el-input v-model.trim="dataForm.label" />
                </el-form-item>
                <el-form-item label="字典值" required>
                    <el-input v-model.trim="dataForm.value" />
                </el-form-item>
                <el-form-item label="排序">
                    <el-input-number v-model="dataForm.sortNo" :min="0" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch v-model="dataForm.status" :active-value="1" :inactive-value="0" />
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model.trim="dataForm.remark" type="textarea" />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="dataDialogVisible = false">取消</el-button>
                    <el-button type="primary" :loading="saving" @click="saveData">保存</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
    createDictData,
    createDictType,
    deleteDictData,
    deleteDictType,
    exportDictData,
    exportDictTypes,
    pageDictData,
    pageDictTypes,
    updateDictData,
    updateDictType,
    type DictDataForm,
    type DictTypeForm,
} from "@/api/system/dict";
import ButtonTable from "@/components/core/tables/button-table/index.vue";
import SearchCard from "@/components/core/tables/search-card/index.vue";
import Table from "@/components/core/tables/table/index.vue";
import TableCard from "@/components/core/tables/table-card/index.vue";
import type { DictDataRow, DictTypeRow } from "@/types/system";
import { formatTime } from "@/utils/format";
import { useFormDraft } from "@/composables/useFormDraft";

defineOptions({ name: "SystemDict" });

const showSearch = ref(false);
const typeLoading = ref(false);
const dataLoading = ref(false);
const saving = ref(false);
const types = ref<DictTypeRow[]>([]);
const dataRows = ref<DictDataRow[]>([]);
const dataTotal = ref(0);
const selectedType = ref<DictTypeRow | null>(null);
const dataTableVisible = ref(false);
const typeDialogVisible = ref(false);
const dataDialogVisible = ref(false);
const editingTypeId = ref<string | null>(null);
const editingDataId = ref<string | null>(null);

const typeQuery = reactive({
    dictName: "",
    dictType: "",
    status: undefined as number | undefined,
});
const typeForm = reactive<DictTypeForm>({
    dictType: "",
    dictName: "",
    status: 1,
    remark: "",
});
const dataQuery = reactive({ pageNum: 1, pageSize: 10 });
const dataForm = reactive<DictDataForm>({
    dictType: "",
    label: "",
    value: "",
    sortNo: 0,
    status: 1,
    remark: "",
});

const typeDraft = useFormDraft({
    key: "dict-type-form",
    form: typeForm,
    visible: typeDialogVisible,
    context: { editingTypeId },
});

const dataDraft = useFormDraft({
    key: "dict-data-form",
    form: dataForm,
    visible: dataDialogVisible,
    context: { editingDataId },
});

async function loadTypes() {
    typeLoading.value = true;
    try {
        const result = await pageDictTypes({
            pageNum: 1,
            pageSize: 100,
            dictName: typeQuery.dictName || undefined,
            dictType: typeQuery.dictType || undefined,
            status: typeQuery.status,
        });
        types.value = result.records;
        if (!selectedType.value && types.value.length) {
            selectedType.value = types.value[0];
            loadData();
        }
    } finally {
        typeLoading.value = false;
    }
}

function searchTypes() {
    selectedType.value = null;
    loadTypes();
}

function resetTypes() {
    typeQuery.dictName = "";
    typeQuery.dictType = "";
    typeQuery.status = undefined;
    searchTypes();
}

function openDataDialog(row: DictTypeRow) {
    selectedType.value = row;
    dataQuery.pageNum = 1;
    dataTableVisible.value = true;
    loadData();
}

async function exportTypes() {
    await exportDictTypes({
        dictName: typeQuery.dictName || undefined,
        dictType: typeQuery.dictType || undefined,
        status: typeQuery.status,
    });
}

async function exportDataRows() {
    if (!selectedType.value) {
        return;
    }
    await exportDictData({ dictType: selectedType.value.dictType });
}

async function loadData() {
    if (!selectedType.value) {
        dataRows.value = [];
        dataTotal.value = 0;
        return;
    }
    dataLoading.value = true;
    try {
        const result = await pageDictData({
            pageNum: dataQuery.pageNum,
            pageSize: dataQuery.pageSize,
            dictType: selectedType.value.dictType,
        });
        dataRows.value = result.records;
        dataTotal.value = Number(result.total);
    } finally {
        dataLoading.value = false;
    }
}

function openTypeCreate() {
    editingTypeId.value = null;
    Object.assign(typeForm, { dictType: "", dictName: "", status: 1, remark: "" });
    typeDialogVisible.value = true;
}

function openTypeEdit(row: DictTypeRow) {
    editingTypeId.value = row.id;
    Object.assign(typeForm, {
        dictType: row.dictType,
        dictName: row.dictName,
        status: row.status,
        remark: row.remark ?? "",
    });
    typeDialogVisible.value = true;
}

async function saveType() {
    if (!typeForm.dictType || !typeForm.dictName) {
        ElMessage.warning("字典类型和名称不能为空");
        return;
    }
    saving.value = true;
    try {
        if (editingTypeId.value) {
            await updateDictType(editingTypeId.value, { ...typeForm });
        } else {
            await createDictType({ ...typeForm });
        }
        ElMessage.success("保存成功");
        typeDraft.clear();
        typeDialogVisible.value = false;
        selectedType.value = null;
        loadTypes();
    } finally {
        saving.value = false;
    }
}

async function removeType(row: DictTypeRow) {
    await ElMessageBox.confirm(`确定删除字典类型「${row.dictName}」吗？`, "提示", {
        type: "warning",
    });
    await deleteDictType(row.id);
    ElMessage.success("删除成功");
    if (selectedType.value?.id === row.id) {
        selectedType.value = null;
    }
    loadTypes();
}

function openDataCreate() {
    if (!selectedType.value) {
        return;
    }
    editingDataId.value = null;
    Object.assign(dataForm, {
        dictType: selectedType.value.dictType,
        label: "",
        value: "",
        sortNo: 0,
        status: 1,
        remark: "",
    });
    dataDialogVisible.value = true;
}

function openDataEdit(row: DictDataRow) {
    editingDataId.value = row.id;
    Object.assign(dataForm, {
        dictType: row.dictType,
        label: row.label,
        value: row.value,
        sortNo: row.sortNo,
        status: row.status,
        remark: row.remark ?? "",
    });
    dataDialogVisible.value = true;
}

async function saveData() {
    if (!dataForm.label || !dataForm.value) {
        ElMessage.warning("显示文本和字典值不能为空");
        return;
    }
    saving.value = true;
    try {
        if (editingDataId.value) {
            await updateDictData(editingDataId.value, { ...dataForm });
        } else {
            await createDictData({ ...dataForm });
        }
        ElMessage.success("保存成功");
        dataDraft.clear();
        dataDialogVisible.value = false;
        loadData();
    } finally {
        saving.value = false;
    }
}

async function removeData(row: DictDataRow) {
    await ElMessageBox.confirm(`确定删除「${row.label}」吗？`, "提示", {
        type: "warning",
    });
    await deleteDictData(row.id);
    ElMessage.success("删除成功");
    loadData();
}

onMounted(loadTypes);
</script>

<style scoped>
.current-type {
    color: var(--text-muted);
    font-size: 13px;
}
</style>
