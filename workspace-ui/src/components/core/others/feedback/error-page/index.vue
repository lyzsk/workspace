<template>
    <div class="error-page">
        <div class="error-card">
            <div class="error-code" :class="tone">{{ code }}</div>
            <h2>{{ title }}</h2>
            <p class="error-desc">{{ description }}</p>
            <div class="error-extra">
                <slot name="content" />
            </div>
            <div class="error-actions">
                <slot name="actions" />
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
withDefaults(
    defineProps<{
        code: string;
        title: string;
        description: string;
        tone?: "warning" | "error" | "success";
    }>(),
    { tone: "warning" },
);
</script>

<style scoped>
.error-page {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 60vh;
    padding: 24px;
}

.error-card {
    width: min(560px, 100%);
    padding: 40px 32px;
    border: 1px solid var(--border);
    border-radius: 16px;
    background: var(--panel-bg);
    text-align: center;
}

.error-code {
    font-size: 64px;
    font-weight: 800;
    line-height: 1;
    letter-spacing: -2px;
    color: var(--primary);
}

.error-code.error {
    color: var(--art-error);
}

.error-code.success {
    color: var(--el-color-success);
}

.error-card h2 {
    margin: 18px 0 8px;
    font-size: 24px;
}

.error-desc {
    margin: 0;
    color: var(--text-muted);
    line-height: 1.7;
}

.error-extra {
    margin-top: 16px;
    color: var(--text-muted);
    font-size: 14px;
    text-align: left;
}

.error-actions {
    display: flex;
    justify-content: center;
    gap: 10px;
    margin-top: 26px;
}
</style>
