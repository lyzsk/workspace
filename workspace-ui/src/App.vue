<template>
  <router-view />
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAppStore } from '@/store/modules/app'

const appStore = useAppStore()
const { locale } = useI18n()

watch(
  () => appStore.locale,
  (value) => {
    locale.value = value
  },
  { immediate: true }
)
</script>
