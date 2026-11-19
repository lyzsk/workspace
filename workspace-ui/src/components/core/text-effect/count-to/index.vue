<template>
    <span class="count-to">{{ formattedValue }}</span>
</template>

<script setup lang="ts">
import { computed, nextTick, shallowRef, watch } from "vue";
import { useTransition, TransitionPresets } from "@vueuse/core";

interface Props {
    target: number;
    duration?: number;
    decimals?: number;
    separator?: string;
    prefix?: string;
    suffix?: string;
    autoStart?: boolean;
    disabled?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
    duration: 1300,
    decimals: 0,
    separator: ",",
    prefix: "",
    suffix: "",
    autoStart: true,
    disabled: false,
});

const emit = defineEmits<{
    started: [value: number];
    finished: [value: number];
    paused: [value: number];
    reset: [];
}>();

const currentValue = shallowRef(0);
const targetValue = shallowRef(props.target);
const isRunning = shallowRef(false);
const isPaused = shallowRef(false);
const pausedValue = shallowRef(0);
const transitionValue = useTransition(currentValue, {
    duration: computed(() => Math.max(100, props.duration)),
    transition: TransitionPresets.easeOutExpo,
    onStarted: () => {
        isRunning.value = true;
        isPaused.value = false;
        emit("started", targetValue.value);
    },
    onFinished: () => {
        isRunning.value = false;
        isPaused.value = false;
        emit("finished", targetValue.value);
    },
});

const formattedValue = computed(() => {
    const value = isPaused.value ? pausedValue.value : transitionValue.value;
    const safe = Number.isFinite(value) ? value : 0;
    const fixed = props.decimals > 0 ? safe.toFixed(props.decimals) : Math.floor(safe).toString();
    const parts = fixed.split(".");
    if (props.separator) {
        parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, props.separator);
    }
    return `${props.prefix}${parts.join(".")}${props.suffix}`;
});

function start(target = targetValue.value) {
    if (props.disabled || !Number.isFinite(target)) {
        return;
    }
    targetValue.value = target;
    if (isPaused.value) {
        currentValue.value = pausedValue.value;
        isPaused.value = false;
    }
    nextTick(() => {
        currentValue.value = target;
    });
}

function pause() {
    if (!isRunning.value || isPaused.value) {
        return;
    }
    isPaused.value = true;
    pausedValue.value = transitionValue.value;
    currentValue.value = pausedValue.value;
    emit("paused", pausedValue.value);
}

function reset(target = 0) {
    currentValue.value = Number.isFinite(target) ? target : 0;
    targetValue.value = currentValue.value;
    isPaused.value = false;
    pausedValue.value = 0;
    emit("reset");
}

function stop() {
    if (isRunning.value || isPaused.value) {
        currentValue.value = transitionValue.value;
        isPaused.value = false;
        isRunning.value = false;
        emit("paused", currentValue.value);
    }
}

function setTarget(target: number) {
    if (!Number.isFinite(target)) {
        return;
    }
    targetValue.value = target;
    if ((isRunning.value || props.autoStart) && !props.disabled) {
        start(target);
    }
}

watch(
    () => props.target,
    (target) => {
        if (props.autoStart && !props.disabled) {
            start(target);
        } else {
            targetValue.value = target;
        }
    },
    { immediate: props.autoStart && !props.disabled },
);

watch(
    () => props.disabled,
    (disabled) => {
        if (disabled) {
            stop();
        }
    },
);

defineExpose({
    start,
    pause,
    reset,
    stop,
    setTarget,
    isRunning: computed(() => isRunning.value),
    isPaused: computed(() => isPaused.value),
    currentValue: computed(() => transitionValue.value),
    targetValue: computed(() => targetValue.value),
    progress: computed(() => targetValue.value === 0
        ? 1
        : Math.abs(transitionValue.value / targetValue.value)),
});
</script>

<style scoped>
.count-to {
    font-variant-numeric: tabular-nums;
}
</style>
