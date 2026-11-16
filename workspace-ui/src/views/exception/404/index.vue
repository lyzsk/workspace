<template>
    <ErrorPage
        code="404"
        title="页面不存在"
        description="你访问的地址可能已经删除、改名，或者输入有误。"
    >
        <template #actions>
            <el-button @click="router.back()">返回上一页</el-button>
            <el-button type="primary" @click="router.replace('/dashboard')">
                返回工作台
            </el-button>
        </template>
    </ErrorPage>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import ErrorPage from "@/components/core/others/feedback/error-page/index.vue";

defineOptions({ name: "Exception404" });

const router = useRouter();
</script>
