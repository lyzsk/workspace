<template>
    <ErrorPage
        code="!"
        title="提交失败"
        description="请核对填写内容或稍后重试；如果问题持续，请联系管理员。"
        tone="error"
    >
        <template #actions>
            <el-button @click="router.back()">返回修改</el-button>
            <el-button type="primary" @click="router.replace('/dashboard')">
                返回工作台
            </el-button>
        </template>
    </ErrorPage>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import ErrorPage from "@/components/core/others/feedback/error-page/index.vue";

defineOptions({ name: "ResultFail" });

const router = useRouter();
</script>
