<template>
    <div class="system-page">
        <SearchCard v-show="showSearch">
            <div class="search-row">
                <div class="search-field">
                    <span class="search-label">缓存 Key</span>
                    <el-input
                        v-model="query.key"
                        placeholder="请输入缓存 Key"
                        clearable
                        style="width: 200px"
                        @keyup.enter="search"
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
                <el-button
                    v-permission="'sys:monitor:cache'"
                    type="primary"
                    @click="openCreate"
                >
                    新增缓存
                </el-button>
                <el-button
                    v-permission="'sys:monitor:cache'"
                    type="danger"
                    plain
                    @click="clear"
                >
                    清空缓存
                </el-button>
            </template>
            <template #default="{ stripe, border, headerBackground, size }">
                <Table
                    v-loading="loading"
                    :data="filteredRows"
                    :stripe="stripe"
                    :border="border"
                    :class="[
                        `table-size-${size}`,
                        { 'no-header-bg': !headerBackground },
                    ]"
                >
                    <el-table-column prop="key" label="缓存 Key" min-width="180" />
                    <el-table-column prop="value" label="缓存值" min-width="220" />
                    <el-table-column label="剩余 TTL" width="120" prop="ttlSeconds">
                        <template #default="{ row }">
                            {{ formatTtl(row.ttlSeconds) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="90" fixed="right" align="center" class-name="action-cell">
                        <template #default="{ row }">
                            <ButtonTable
                                v-permission="'sys:monitor:cache'"
                                type="delete"
                                @click="remove(row)"
                            />
                        </template>
                    </el-table-column>
                </Table>
            </template>
        </TableCard>

        <el-dialog draggable v-model="dialogVisible" title="新增缓存" width="420px">
            <el-form :model="form" label-width="100px">
                <el-form-item label="缓存 Key" required>
                    <el-input v-model.trim="form.key" />
                </el-form-item>
                <el-form-item label="缓存值">
                    <el-input v-model.trim="form.value" type="textarea" />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="dialogVisible = false">取消</el-button>
                    <el-button type="primary" :loading="saving" @click="save">保存</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
    clearCache,
    deleteCacheKey,
    getCacheKeys,
    putCache,
    type CacheEntry,
} from "@/api/system/monitor";
import ButtonTable from "@/components/core/tables/button-table/index.vue";
import SearchCard from "@/components/core/tables/search-card/index.vue";
import Table from "@/components/core/tables/table/index.vue";
import TableCard from "@/components/core/tables/table-card/index.vue";
import { useFormDraft } from "@/composables/useFormDraft";

defineOptions({ name: "MonitorCacheList" });

const showSearch = ref(false);
const loading = ref(false);
const saving = ref(false);
const rows = ref<CacheEntry[]>([]);
const dialogVisible = ref(false);
const query = reactive({ key: "" });
const form = reactive({ key: "", value: "" });

const draft = useFormDraft({
    key: "cache-form",
    form,
    visible: dialogVisible,
});

const filteredRows = computed(() =>
    rows.value.filter((row) => !query.key || row.key.includes(query.key)),
);

function formatTtl(seconds: number) {
    if (seconds === -1) {
        return "永久";
    }
    if (seconds === -2) {
        return "不存在";
    }
    if (seconds < 0) {
        return "未知";
    }
    return `${seconds}s`;
}

async function load() {
    loading.value = true;
    try {
        rows.value = await getCacheKeys();
    } finally {
        loading.value = false;
    }
}

function search() {
}

function reset() {
    query.key = "";
}

function openCreate() {
    form.key = "";
    form.value = "";
    dialogVisible.value = true;
}

async function save() {
    if (!form.key) {
        ElMessage.warning("缓存 Key 不能为空");
        return;
    }
    saving.value = true;
    try {
        await putCache(form.key, form.value);
        ElMessage.success("保存成功");
        draft.clear();
        dialogVisible.value = false;
        load();
    } finally {
        saving.value = false;
    }
}

async function remove(row: CacheEntry) {
    await ElMessageBox.confirm(`确定删除缓存「${row.key}」吗？`, "提示", {
        type: "warning",
    });
    await deleteCacheKey(row.key);
    ElMessage.success("删除成功");
    load();
}

async function clear() {
    await ElMessageBox.confirm("确定清空全部缓存吗？", "提示", {
        type: "warning",
    });
    await clearCache();
    ElMessage.success("已清空");
    load();
}

onMounted(load);
</script>
