<template>
    <div class="cron-builder">
        <div class="cron-presets">
            <el-select v-model="preset" placeholder="选择常用规则" @change="applyPreset">
                <el-option
                    v-for="item in presets"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                />
            </el-select>
            <el-button @click="validate">校验并预览</el-button>
            <span class="cron-expression">{{ modelValue }}</span>
        </div>
        <div class="cron-fields">
            <el-input v-model="fields.second" addon-before="秒" />
            <el-input v-model="fields.minute" addon-before="分" />
            <el-input v-model="fields.hour" addon-before="时" />
            <el-input v-model="fields.dayOfMonth" addon-before="日" />
            <el-input v-model="fields.month" addon-before="月" />
            <el-input v-model="fields.dayOfWeek" addon-before="周" />
        </div>
        <div class="cron-summary">{{ summary }}</div>
        <el-alert
            v-if="validation"
            :title="validation.message"
            :type="validation.valid ? 'success' : 'error'"
            :closable="false"
            show-icon
        />
        <div v-if="validation?.valid" class="cron-next">
            <div>未来 5 次执行时间：</div>
            <div v-for="item in validation.nextTimes" :key="item">
                {{ formatTime(item) }}
            </div>
            <div class="cron-rule">{{ validation.rule }}</div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { validateCronExpression, type CronValidation } from "@/api/system/job";
import { formatTime } from "@/utils/format";

const props = defineProps<{ modelValue: string }>();
const emit = defineEmits<{
    "update:modelValue": [value: string];
}>();

const presets = [
    { label: "每分钟", value: "0 * * * * ?", second: "0", minute: "*", hour: "*", dayOfMonth: "*", month: "*", dayOfWeek: "?" },
    { label: "每小时", value: "0 0 * * * ?", second: "0", minute: "0", hour: "*", dayOfMonth: "*", month: "*", dayOfWeek: "?" },
    { label: "每天 00:00", value: "0 0 0 * * ?", second: "0", minute: "0", hour: "0", dayOfMonth: "*", month: "*", dayOfWeek: "?" },
    { label: "每天 12:00", value: "0 0 12 * * ?", second: "0", minute: "0", hour: "12", dayOfMonth: "*", month: "*", dayOfWeek: "?" },
    { label: "工作日 00:00", value: "0 0 0 * * MON-FRI", second: "0", minute: "0", hour: "0", dayOfMonth: "*", month: "*", dayOfWeek: "MON-FRI" },
    { label: "每周一 00:00", value: "0 0 0 * * 2", second: "0", minute: "0", hour: "0", dayOfMonth: "*", month: "*", dayOfWeek: "2" },
    { label: "每月 1 日 00:00", value: "0 0 0 1 * ?", second: "0", minute: "0", hour: "0", dayOfMonth: "1", month: "*", dayOfWeek: "?" },
    { label: "每 5 分钟", value: "0 0/5 * * * ?", second: "0", minute: "0/5", hour: "*", dayOfMonth: "*", month: "*", dayOfWeek: "?" },
];

const fields = reactive({
    second: "0",
    minute: "*",
    hour: "*",
    dayOfMonth: "*",
    month: "*",
    dayOfWeek: "?",
});
const preset = ref("");
const validation = ref<CronValidation>();
const expression = computed(() =>
    [
        fields.second,
        fields.minute,
        fields.hour,
        fields.dayOfMonth,
        fields.month,
        fields.dayOfWeek,
    ].join(" "),
);
const summary = computed(() => {
    const matched = presets.find((item) => item.value === expression.value);
    return matched ? `当前规则：${matched.label}` : `自定义规则：${expression.value}`;
});

watch(expression, (value) => {
    emit("update:modelValue", value);
    validation.value = undefined;
});

watch(
    () => props.modelValue,
    (value) => {
        if (value !== expression.value) {
            parse(value);
        }
    },
    { immediate: true },
);

function parse(value: string) {
    const parts = value.trim().split(/\s+/);
    if (parts.length !== 6) {
        return;
    }
    [fields.second, fields.minute, fields.hour, fields.dayOfMonth, fields.month, fields.dayOfWeek] = parts;
}

function applyPreset(value: string) {
    const item = presets.find((candidate) => candidate.value === value);
    if (!item) {
        return;
    }
    fields.second = item.second;
    fields.minute = item.minute;
    fields.hour = item.hour;
    fields.dayOfMonth = item.dayOfMonth;
    fields.month = item.month;
    fields.dayOfWeek = item.dayOfWeek;
}

async function validate() {
    validation.value = await validateCronExpression(expression.value);
}
</script>

<style scoped>
.cron-builder {
    display: flex;
    flex-direction: column;
    gap: 10px;
    width: 100%;
}

.cron-presets {
    display: flex;
    gap: 8px;
    align-items: center;
}

.cron-presets .el-select {
    width: 190px;
}

.cron-expression {
    color: var(--text-muted);
    font-family: monospace;
}

.cron-fields {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 6px;
}

.cron-summary,
.cron-rule {
    color: var(--text-muted);
    font-size: 13px;
}

.cron-next {
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 13px;
}
</style>
