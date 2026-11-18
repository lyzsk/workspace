<template>
    <div class="dashboard">
        <h1>
            {{ t("dashboard.welcome") }},
            {{ userStore.user?.realName || userStore.user?.username }}
        </h1>
        <p class="desc">{{ t("dashboard.welcomeDesc") }}</p>

        <el-row :gutter="16">
            <el-col :xs="24" :sm="8">
                <el-card>
                    <div class="card-label">{{ t("dashboard.user") }}</div>
                    <div class="card-value">
                        {{
                            userStore.user?.realName || userStore.user?.username
                        }}
                    </div>
                </el-card>
            </el-col>
            <el-col :xs="24" :sm="8">
                <el-card>
                    <div class="card-label">{{ t("dashboard.tenant") }}</div>
                    <div class="card-value">
                        {{ userStore.user?.tenantId || "0" }}
                    </div>
                </el-card>
            </el-col>
            <el-col :xs="24" :sm="8">
                <el-card>
                    <div class="card-label">{{ t("dashboard.status") }}</div>
                    <div class="card-value green">
                        {{ t("dashboard.normal") }}
                    </div>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts">
import { useI18n } from "vue-i18n";
import { useUserStore } from "@/store/modules/user";

const { t } = useI18n();
const userStore = useUserStore();
</script>

<style scoped lang="scss">
.dashboard h1 {
    margin: 0;
    font-size: 26px;
}

.desc {
    margin: 8px 0 24px;
    color: var(--text-muted);
}

.card-label {
    color: var(--text-muted);
    font-size: 13px;
}

.card-value {
    margin-top: 10px;
    font-size: 22px;
    font-weight: 600;
}

.card-value.green {
    color: var(--el-color-success);
}

:deep(.el-card) {
    border-color: var(--border);
    background: var(--panel-bg);
    color: var(--text);
}
</style>
