<template>
    <div class="page-header">
        <div class="page-header-text">
            <h1>{{ title }}</h1>
            <p v-if="description">{{ description }}</p>
        </div>
        <div class="page-header-actions">
            <slot />
        </div>
    </div>
</template>

<script setup lang="ts">
defineProps<{
    title: string;
    description?: string;
}>();
</script>
