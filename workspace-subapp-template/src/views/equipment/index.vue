<template>
    <div class="page-container">
        <!-- 搜索卡片：默认收起，点表格工具栏的搜索图标才展开，与主应用一致 -->
        <section v-show="showSearch" class="page-toolbar">
            <div class="search-field">
                <span class="search-label">设备关键字</span>
                <el-input
                    v-model="query.keyword"
                    placeholder="输入设备编号或名称"
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
                    <span class="table-hint">
                        当前用户：{{ userStore.displayName }}
                        <template v-if="userStore.hasRole('SUPER_ADMIN')">（超级管理员）</template>
                    </span>
                </div>
                <div class="table-toolbar-right">
                    <el-button
                        text
                        :icon="Search"
                        :title="showSearch ? '收起查询' : '查询'"
                        @click="showSearch = !showSearch"
                    />
                    <el-button text :icon="Refresh" title="刷新" @click="loadData" />
                </div>
            </div>

            <!--
                表格参数与主应用保持一致：默认只有斑马纹，不画单元格竖线。
                外框由外层表格卡片提供，表格自己再套一圈会出现双层描边。
            -->
            <el-table v-loading="loading" :data="rows" stripe>
                <el-table-column prop="code" label="设备编号" width="140" />
                <el-table-column prop="name" label="设备名称" min-width="180" />
                <el-table-column prop="model" label="型号" width="140" />
                <el-table-column prop="status" label="状态" width="110">
                    <template #default="{ row }">
                        <el-tag :type="statusTagType(row.status)" size="small">
                            {{ row.status }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="owner" label="责任人" width="120" />
                <el-table-column prop="updateTime" label="更新时间" width="180" />
            </el-table>

            <div class="table-pagination">
                <el-pagination
                    v-model:current-page="query.pageNum"
                    v-model:page-size="query.pageSize"
                    :total="total"
                    layout="total, sizes, prev, pager, next"
                    background
                    @current-change="loadData"
                    @size-change="loadData"
                />
            </div>
        </section>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { Refresh, Search } from "@element-plus/icons-vue";
import { listEquipment, type Equipment, type EquipmentQuery } from "@/api/equipment";
import { useUserStore } from "@/store/modules/user";

/**
 * 设备清单示例页。
 *
 * 这一页的作用是演示模板的完整用法，不是真实业务：
 * 1. 接口调用走 api 层，页面不认识 axios
 * 2. 用户信息从 store 取，store 的数据来自主应用注入
 * 3. 只输出内容区，没有自己的标题栏和侧边栏
 */

const userStore = useUserStore();

const loading = ref(false);
/** 查询区默认收起，点工具栏的搜索图标才展开，与主应用行为一致 */
const showSearch = ref(false);
const rows = ref<Equipment[]>([]);
const total = ref(0);

const query = reactive<EquipmentQuery>({
    pageNum: 1,
    pageSize: 10,
    keyword: "",
});

/** 状态对应的标签颜色 */
function statusTagType(status: Equipment["status"]) {
    if (status === "运行") {
        return "success";
    }
    if (status === "维修") {
        return "warning";
    }
    if (status === "停用") {
        return "info";
    }
    return "primary";
}

/** 加载列表数据 */
async function loadData() {
    loading.value = true;
    try {
        const result = await listEquipment(query);
        rows.value = result.rows;
        total.value = result.total;
    } catch (error) {
        // 会话过期、无权限这类错误已在 http 层统一上报，这里只做兜底提示
        console.error("[equipment] 加载设备清单失败", error);
        rows.value = [];
        total.value = 0;
    } finally {
        loading.value = false;
    }
}

/** 查询：回到第一页 */
function handleSearch() {
    query.pageNum = 1;
    loadData();
}

/** 重置查询条件 */
function handleReset() {
    query.keyword = "";
    query.pageNum = 1;
    loadData();
}

onMounted(() => {
    loadData();
});
</script>
