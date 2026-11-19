<template>
    <div ref="chartElement" class="dashboard-chart"></div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, shallowRef, watch } from "vue";
import * as echarts from "echarts";
import type { ECharts, EChartsOption } from "echarts";
import { useAppStore } from "@/store/modules/app";

const props = withDefaults(defineProps<{
    option: EChartsOption;
    height?: string;
}>(), {
    height: "300px",
});

const chartElement = ref<HTMLElement>();
const chart = shallowRef<ECharts>();
const appStore = useAppStore();
let observer: ResizeObserver | undefined;

function render() {
    if (!chartElement.value) {
        return;
    }
    chartElement.value.style.height = props.height;
    if (!chart.value) {
        chart.value = echarts.init(chartElement.value);
    }
    chart.value.setOption({
        textStyle: {
            color: getComputedStyle(chartElement.value).getPropertyValue("--text-muted").trim(),
        },
        ...props.option,
    }, true);
}

watch(() => props.option, render, { deep: true });
watch(() => appStore.isDark, render);
watch(() => props.height, render);

onMounted(() => {
    render();
    observer = new ResizeObserver(() => chart.value?.resize());
    if (chartElement.value) {
        observer.observe(chartElement.value);
    }
});

onUnmounted(() => {
    observer?.disconnect();
    chart.value?.dispose();
});
</script>

<style scoped>
.dashboard-chart {
    width: 100%;
    min-width: 0;
}
</style>
