<template>
    <div class="page-container">
        <!-- 搜索卡片：默认收起，与设备清单保持一致 -->
        <section v-show="showSearch" class="page-toolbar">
            <div class="search-field">
                <span class="search-label">设备编号</span>
                <el-input
                    v-model="keyword"
                    placeholder="输入设备编号检索"
                    clearable
                    style="width: 220px"
                    @keyup.enter="handleSearch"
                />
            </div>
            <div class="search-actions">
                <el-button type="primary" @click="handleSearch">查询</el-button>
                <el-button @click="handleReset">重置</el-button>
            </div>
        </section>

        <!-- 表格卡片：顶部工具行 + 表格 + 右对齐分页 -->
        <section class="page-body">
            <div class="table-toolbar">
                <div class="table-toolbar-left">
                    <span class="table-hint">台账按存放位置归集，用于盘点与调拨</span>
                </div>
                <div class="table-toolbar-right">
                    <el-button
                        text
                        :icon="Search"
                        :title="showSearch ? '收起查询' : '查询'"
                        @click="showSearch = !showSearch"
                    />
                    <el-button text :icon="Refresh" title="刷新" @click="handleSearch" />
                </div>
            </div>

            <el-table :data="pagedRows" stripe>
                <el-table-column prop="code" label="设备编号" width="140" />
                <el-table-column prop="name" label="设备名称" min-width="160" />
                <el-table-column prop="location" label="存放位置" min-width="140" />
                <el-table-column prop="dept" label="归属部门" width="130" />
                <el-table-column prop="assetNo" label="资产编号" width="150" />
                <el-table-column prop="entryDate" label="入账日期" width="130" />
            </el-table>

            <div class="table-pagination">
                <el-pagination
                    v-model:current-page="pageNum"
                    v-model:page-size="pageSize"
                    :total="filteredRows.length"
                    layout="total, sizes, prev, pager, next"
                    background
                />
            </div>
        </section>
    </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { Refresh, Search } from "@element-plus/icons-vue";

/**
 * 设备台账示例页。
 *
 * 用于验证主应用菜单多级挂载与子路由切换，数据为静态演示数据。
 * 接入真实接口时，照 views/equipment/index.vue 的写法在 api 层加函数。
 *
 * 页面骨架与设备清单、保养记录保持一致：
 * 搜索卡片默认收起 → 表格卡片（工具行 + 表格 + 分页）。
 */

interface LedgerRow {
    code: string;
    name: string;
    location: string;
    dept: string;
    assetNo: string;
    entryDate: string;
}

/** 查询区默认收起，点工具栏的搜索图标才展开 */
const showSearch = ref(false);
const keyword = ref("");
const pageNum = ref(1);
const pageSize = ref(10);

const rows: LedgerRow[] = [
    { code: "EQ-0001", name: "刻蚀机 A", location: "一号厂房 2F", dept: "工艺部", assetNo: "ZC-2026-0001", entryDate: "2026-03-12" },
    { code: "EQ-0002", name: "薄膜沉积设备", location: "一号厂房 2F", dept: "工艺部", assetNo: "ZC-2026-0002", entryDate: "2026-03-12" },
    { code: "EQ-0003", name: "量测显微镜", location: "实验楼 1F", dept: "质量部", assetNo: "ZC-2026-0003", entryDate: "2026-04-08" },
    { code: "EQ-0004", name: "清洗机", location: "一号厂房 1F", dept: "制造部", assetNo: "ZC-2026-0004", entryDate: "2026-04-19" },
    { code: "EQ-0005", name: "真空泵组", location: "动力站", dept: "设备部", assetNo: "ZC-2026-0005", entryDate: "2026-05-06" },
];

/** 按关键字过滤后的全量结果 */
const filteredRows = computed(() => {
    const value = keyword.value.trim().toLowerCase();
    if (!value) {
        return rows;
    }
    return rows.filter((row) => row.code.toLowerCase().includes(value));
});

/** 当前页展示的数据 */
const pagedRows = computed(() => {
    const start = (pageNum.value - 1) * pageSize.value;
    return filteredRows.value.slice(start, start + pageSize.value);
});

/** 查询：回到第一页 */
function handleSearch() {
    pageNum.value = 1;
}

/** 重置查询条件 */
function handleReset() {
    keyword.value = "";
    pageNum.value = 1;
}
</script>
