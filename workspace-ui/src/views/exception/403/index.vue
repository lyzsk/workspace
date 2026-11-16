<template>
    <ErrorPage
        code="403"
        title="没有访问权限"
        description="当前账号没有访问该页面的权限，请联系管理员分配角色或菜单权限。"
        tone="error"
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

defineOptions({ name: "Exception403" });

const router = useRouter();
</script>
