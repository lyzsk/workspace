<template>
    <div v-loading="loading" class="dashboard-page">
        <el-row :gutter="20">
            <el-col :xs="24" :lg="10">
                <div class="dashboard-card">
                    <DashboardChart :option="applicationBarOption" height="240px" />
                    <h3 class="overview-title">应用访问数</h3>
                    <p class="overview-description">
                        比上周
                        <span :class="changeClass(data.visitsWeekChange)">
                            {{ formatChange(data.visitsWeekChange) }}
                        </span>
                    </p>
                    <div class="overview-metrics">
                        <div>
                            <strong><CountTo :target="metric(data.totalUsers)" /></strong>
                            <span>总用户量</span>
                        </div>
                        <div>
                            <strong><CountTo :target="metric(data.totalVisits)" /></strong>
                            <span>总访问量</span>
                        </div>
                        <div>
                            <strong><CountTo :target="metric(data.todayVisits)" /></strong>
                            <span>日访问量</span>
                        </div>
                        <div>
                            <strong :class="changeClass(data.visitsWeekChange)">
                                {{ formatChange(data.visitsWeekChange) }}
                            </strong>
                            <span>周同比</span>
                        </div>
                    </div>
                </div>
            </el-col>
            <el-col :xs="24" :lg="14">
                <div class="dashboard-card">
                    <div class="card-header">
                        <h3>访问量</h3>
                        <span>
                            近 7 日 · 较上周
                            <b :class="changeClass(data.visitsWeekChange)">
                                {{ formatChange(data.visitsWeekChange) }}
                            </b>
                        </span>
                    </div>
                    <DashboardChart :option="weekTrendOption" height="330px" />
                </div>
            </el-col>
        </el-row>

        <el-row :gutter="20">
            <el-col :xs="24" :lg="24">
                <div class="dashboard-card">
                    <div class="card-header">
                        <h3>TOP 访问用户</h3>
                        <span>本周</span>
                    </div>
                    <el-table :data="data.topUsers" :stripe="false" size="small">
                        <el-table-column label="用户" min-width="110">
                            <template #default="{ row }">
                                {{ row.realName || row.username }}
                            </template>
                        </el-table-column>
                        <el-table-column prop="departmentName" label="部门" min-width="100" show-overflow-tooltip />
                        <el-table-column prop="visits" label="访问量" width="72" align="right" />
                    </el-table>
                    <el-empty v-if="!data.topUsers.length" description="暂无访问用户" :image-size="64" />
                </div>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import type { EChartsOption } from "echarts";
import CountTo from "@/components/core/text-effect/count-to/index.vue";
import DashboardChart from "../components/DashboardChart.vue";
import {
    getDashboard,
    metric,
    type DashboardData,
} from "@/api/system/dashboard";

const emptyData: DashboardData = {
    totalVisits: 0,
    todayVisits: 0,
    weekApiCalls: 0,
    todayUv: 0,
    onlineUsers: 0,
    totalUsers: 0,
    totalApps: 0,
    enabledApps: 0,
    visitsWeekChange: 0,
    todayVisitsWeekChange: 0,
    apiCallsWeekChange: 0,
    activeUsersWeekChange: 0,
    usersWeekChange: 0,
    weekTrend: [],
    userGrowth: [],
    apiCallTrend: [],
    appRanking: [],
    topUsers: [],
    userDepartments: [],
    appDepartments: [],
};
const data = ref<DashboardData>({ ...emptyData });
const loading = ref(false);
let timer: ReturnType<typeof setInterval> | undefined;
const applicationBarOption = computed<EChartsOption>(() => ({
    animationDuration: 1000,
    grid: { left: 12, right: 12, top: 20, bottom: 10, containLabel: true },
    tooltip: { trigger: "axis" },
    legend: { data: ["访问量"], right: 0 },
    xAxis: {
        type: "category",
        data: data.value.appRanking.map(item => item.appName),
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { interval: 0, rotate: data.value.appRanking.length > 4 ? 20 : 0 },
    },
    yAxis: { type: "value", minInterval: 1, splitLine: { lineStyle: { opacity: 0.12 } } },
    series: [{
        name: "访问量",
        type: "bar",
        data: data.value.appRanking.map(item => metric(item.visits)),
        barWidth: "26%",
        itemStyle: { color: "#5d87ff", borderRadius: [4, 4, 0, 0] },
    }],
}));

const weekTrendOption = computed<EChartsOption>(() => ({
    animationDuration: 1200,
    color: ["#5d87ff", "#00c2a8"],
    grid: { left: 12, right: 18, top: 30, bottom: 34, containLabel: true },
    tooltip: { trigger: "axis" },
    legend: { data: ["PV", "UV"], bottom: 0 },
    xAxis: {
        type: "category",
        boundaryGap: false,
        data: data.value.weekTrend.map(item => item.day.slice(5)),
        axisLine: { show: false },
        axisTick: { show: false },
    },
    yAxis: { type: "value", minInterval: 1, splitLine: { lineStyle: { opacity: 0.12 } } },
    series: [
        {
            name: "PV",
            type: "line",
            smooth: true,
            showSymbol: false,
            data: data.value.weekTrend.map(item => metric(item.pv)),
        },
        {
            name: "UV",
            type: "line",
            smooth: true,
            showSymbol: false,
            data: data.value.weekTrend.map(item => metric(item.uv)),
        },
    ],
}));

function formatChange(value: number) {
    return `${value > 0 ? "+" : ""}${value.toFixed(1)}%`;
}

function changeClass(value: number) {
    return value > 0 ? "change-up" : value < 0 ? "change-down" : "";
}

async function load() {
    loading.value = true;
    try {
        data.value = await getDashboard();
    } finally {
        loading.value = false;
    }
}

onMounted(() => {
    void load();
    timer = setInterval(() => void load(), 60000);
});

onUnmounted(() => clearInterval(timer));
</script>

<style scoped lang="scss" src="../dashboard.scss"></style>
