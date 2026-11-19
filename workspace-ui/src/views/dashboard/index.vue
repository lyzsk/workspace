<template>
    <div v-loading="loading" class="dashboard-page">
        <el-row :gutter="20">
            <el-col v-for="card in cards" :key="card.label" :xs="24" :sm="12" :lg="6">
                <div class="dashboard-card metric-card">
                    <div class="metric-label">{{ card.label }}</div>
                    <CountTo class="metric-value" :target="card.value" />
                    <div class="metric-change">
                        较上周
                        <span :class="changeClass(card.change)">{{ formatChange(card.change) }}</span>
                    </div>
                    <DashboardChart
                        class="metric-mini-chart"
                        :option="miniChartOption(card.trend, card.color)"
                        height="60px"
                    />
                </div>
            </el-col>
        </el-row>

        <el-row :gutter="20">
            <el-col :xs="24" :lg="10">
                <div class="dashboard-card">
                    <div class="card-header"><h3>用户部门分布</h3></div>
                    <DashboardChart :option="userDepartmentOption" height="300px" />
                </div>
            </el-col>
            <el-col :xs="24" :lg="14">
                <div class="dashboard-card">
                    <div class="card-header">
                        <h3>热门应用</h3>
                        <span>本周访问量</span>
                    </div>
                    <div class="hot-app-list">
                        <div
                            v-for="app in hotApplications"
                            :key="app.appCode"
                            class="hot-app-card"
                        >
                            <div class="hot-app-header">
                                <div class="hot-app-icon" :style="{ color: app.color }">
                                    <SvgIcon icon="ri:apps-line" />
                                </div>
                                <div class="hot-app-value">
                                    <CountTo :target="app.visits" />
                                    <span>{{ app.appName }}</span>
                                </div>
                            </div>
                            <el-progress
                                :percentage="app.popularity"
                                :stroke-width="5"
                                :show-text="false"
                                :color="app.color"
                            />
                        </div>
                    </div>
                    <el-empty v-if="!hotApplications.length" description="暂无访问数据" :image-size="64" />
                </div>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import type { EChartsOption } from "echarts";
import CountTo from "@/components/core/text-effect/count-to/index.vue";
import SvgIcon from "@/components/core/base/svg-icon/index.vue";
import DashboardChart from "./components/DashboardChart.vue";
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

const cards = computed(() => [
    {
        label: "总访问次数",
        value: metric(data.value.totalVisits),
        change: data.value.visitsWeekChange,
        color: "#5d87ff",
        trend: data.value.weekTrend.map(item => metric(item.pv)),
    },
    {
        label: "在线用户数",
        value: metric(data.value.onlineUsers),
        change: data.value.activeUsersWeekChange,
        color: "#00c2a8",
        trend: data.value.weekTrend.map(item => metric(item.uv)),
    },
    {
        label: "近7日点击量",
        value: metric(data.value.weekApiCalls),
        change: data.value.apiCallsWeekChange,
        color: "#884cff",
        trend: data.value.apiCallTrend.map(item => metric(item.value)),
    },
    {
        label: "总用户数",
        value: metric(data.value.totalUsers),
        change: data.value.usersWeekChange,
        color: "#fe8f0e",
        trend: data.value.userGrowth.map(item => metric(item.value)),
    },
]);

const colors = ["#00e096", "#0095ff", "#884cff", "#fe8f0e", "#f9506e"];

const hotApplications = computed(() => {
    const max = Math.max(0, ...data.value.appRanking.map(item => metric(item.visits)));
    return data.value.appRanking.map((item, index) => ({
        ...item,
        visits: metric(item.visits),
        popularity: max === 0 ? 0 : Math.round(metric(item.visits) * 100 / max),
        color: colors[index % colors.length],
    }));
});

const userDepartmentOption = computed<EChartsOption>(() => ringOption(
    data.value.userDepartments.map(item => ({
        name: item.name,
        value: metric(item.value),
    })),
));

function miniChartOption(values: number[], color: string): EChartsOption {
    return {
        animationDuration: 800,
        grid: { left: 0, right: 0, top: 0, bottom: 0 },
        xAxis: { type: "category", show: false, boundaryGap: false },
        yAxis: { type: "value", show: false },
        series: [{
            type: "line",
            data: values,
            smooth: true,
            showSymbol: false,
            lineStyle: { width: 3, color },
            areaStyle: { color, opacity: 0.12 },
        }],
    };
}

function ringOption(values: Array<{ name: string; value: number }>): EChartsOption {
    return {
        animationDuration: 1000,
        color: ["#4c87f3", "#93f1b4", "#8bd8fc", "#884cff", "#fe8f0e"],
        tooltip: { trigger: "item", formatter: "{b}: {c} ({d}%)" },
        legend: { type: "scroll", bottom: 0 },
        series: [{
            type: "pie",
            radius: ["54%", "70%"],
            center: ["50%", "42%"],
            data: values,
            itemStyle: { borderRadius: 4, borderWidth: 2 },
            label: { show: false },
        }],
    };
}

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

<style scoped lang="scss" src="./dashboard.scss"></style>
