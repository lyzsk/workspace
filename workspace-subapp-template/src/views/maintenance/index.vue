<template>
    <div class="page-container">
        <!-- 搜索卡片：默认收起，与设备清单保持一致 -->
        <section v-show="showSearch" class="page-toolbar">
            <div class="search-field">
                <span class="search-label">保养状态</span>
                <el-select v-model="statusFilter" placeholder="请选择状态" style="width: 160px">
                    <el-option label="全部" value="全部" />
                    <el-option label="待保养" value="待保养" />
                    <el-option label="已完成" value="已完成" />
                </el-select>
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
                    <span class="table-hint">保养到期前 3 天自动提醒责任人</span>
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
                <el-table-column prop="planDate" label="计划日期" width="130" />
                <el-table-column prop="cycle" label="保养周期" width="110" />
                <el-table-column prop="owner" label="责任人" width="110" />
                <el-table-column prop="status" label="状态" width="110">
                    <template #default="{ row }">
                        <el-tag :type="row.status === '已完成' ? 'success' : 'warning'" size="small">
                            {{ row.status }}
                        </el-tag>
                    </template>
                </el-table-column>
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
 * 保养记录示例页。
 *
 * 同样是用于验证菜单与子路由的演示页面，数据为静态内容。
 * 页面骨架与设备清单、设备台账保持一致：
 * 搜索卡片默认收起 → 表格卡片（工具行 + 表格 + 分页）。
 */

interface MaintenanceRow {
    code: string;
    name: string;
    planDate: string;
    cycle: string;
    owner: string;
    status: "待保养" | "已完成";
}

/** 查询区默认收起，点工具栏的搜索图标才展开 */
const showSearch = ref(false);
const statusFilter = ref("全部");
const pageNum = ref(1);
const pageSize = ref(10);

const rows: MaintenanceRow[] = [
    { code: "EQ-0001", name: "刻蚀机 A", planDate: "2026-11-22", cycle: "季度", owner: "张工", status: "待保养" },
    { code: "EQ-0002", name: "薄膜沉积设备", planDate: "2026-11-25", cycle: "季度", owner: "李工", status: "待保养" },
    { code: "EQ-0003", name: "量测显微镜", planDate: "2026-11-15", cycle: "半年", owner: "王工", status: "已完成" },
    { code: "EQ-0004", name: "清洗机", planDate: "2026-11-28", cycle: "月度", owner: "赵工", status: "待保养" },
    { code: "EQ-0005", name: "真空泵组", planDate: "2026-11-10", cycle: "月度", owner: "孙工", status: "已完成" },
];

/** 按状态过滤后的全量结果 */
const filteredRows = computed(() => {
    if (statusFilter.value === "全部") {
        return rows;
    }
    return rows.filter((row) => row.status === statusFilter.value);
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
    statusFilter.value = "全部";
    pageNum.value = 1;
}
</script>
