<template>
    <ErrorPage
        code="500"
        title="服务开小差了"
        description="服务器处理请求时出现异常，请稍后重试；如果持续出现，请联系管理员查看后端日志。"
        tone="error"
    >
        <template #actions>
            <el-button @click="reload">刷新页面</el-button>
            <el-button type="primary" @click="router.replace('/dashboard')">
                返回工作台
            </el-button>
        </template>
    </ErrorPage>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import ErrorPage from "@/components/core/others/feedback/error-page/index.vue";

defineOptions({ name: "Exception500" });

const router = useRouter();

function reload() {
    window.location.reload();
}
</script>
