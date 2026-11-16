<template>
    <ErrorPage
        code="✓"
        title="提交成功"
        description="操作已经完成，可以返回工作台继续处理其他事项。"
        tone="success"
    >
        <template #content>
            <p>如需查看结果，请到对应列表页确认数据。</p>
        </template>
        <template #actions>
            <el-button type="primary" @click="router.replace('/dashboard')">
                返回工作台
            </el-button>
        </template>
    </ErrorPage>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import ErrorPage from "@/components/core/others/feedback/error-page/index.vue";

defineOptions({ name: "ResultSuccess" });

const router = useRouter();
</script>
