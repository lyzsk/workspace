<template>
    <div v-loading="loading" class="app-grant-panel">
        <Table
            v-if="grants.length"
            :data="grantTable"
            row-key="id"
            :tree-props="{ children: 'children' }"
            :stripe="false"
            size="small"
            class="app-grant-table"
        >
            <el-table-column label="应用" min-width="150" show-overflow-tooltip>
                <template #default="{ row }">
                    <strong v-if="row.kind === 'application'" class="app-grant-name">
                        {{ row.label }}
                    </strong>
                </template>
            </el-table-column>
            <el-table-column label="授权项" width="100">
                <template #default="{ row }">
                    <span v-if="row.kind === 'detail'" class="app-grant-detail">
                        {{ row.label }}
                    </span>
                </template>
            </el-table-column>
            <el-table-column label="编码" min-width="150" show-overflow-tooltip>
                <template #default="{ row }">
                    <span v-if="row.kind === 'application'" class="app-grant-code">
                        {{ row.value }}
                    </span>
                </template>
            </el-table-column>
            <el-table-column label="授权内容" min-width="160" show-overflow-tooltip>
                <template #default="{ row }">
                    <span v-if="row.kind === 'detail'" class="app-grant-value">
                        {{ row.value }}
                    </span>
                </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
                <template #default="{ row }">
                    <el-tag
                        v-if="row.kind === 'application'"
                        size="small"
                        effect="light"
                        :type="row.grant.effective ? 'success' : 'info'"
                    >
                        {{ statusLabel(row.grant) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center" class-name="action-cell">
                <template #default="{ row }">
                    <ButtonTable
                        v-if="row.kind === 'application'"
                        v-permission="'sys:user:app-grant'"
                        icon="ri:settings-3-line"
                        icon-class="bg-theme/12 text-theme"
                        title="配置"
                        aria-label="配置"
                        @click="openConfig(row.grant)"
                    />
                </template>
            </el-table-column>
        </Table>
        <el-empty v-if="!loading && !grants.length" description="暂无应用" :image-size="48" />

        <el-dialog v-model="visible" :title="`配置 ${form.appName}`" width="560px" append-to-body>
            <el-form label-width="100px">
                <el-form-item label="应用状态">
                    <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
                </el-form-item>
                <el-form-item label="应用角色">
                    <el-select v-model="form.roleIds" multiple clearable style="width: 100%">
                        <el-option
                            v-for="role in roleOptions"
                            :key="role.id"
                            :label="role.roleName"
                            :value="role.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="数据权限">
                    <el-select v-model="form.dataScope" style="width: 100%">
                        <el-option label="跟随应用角色" :value="0" />
                        <el-option label="全部数据" :value="1" />
                        <el-option label="指定部门" :value="2" />
                        <el-option label="本部门" :value="3" />
                        <el-option label="本部门及以下" :value="4" />
                        <el-option label="仅本人" :value="5" />
                    </el-select>
                </el-form-item>
                <el-form-item v-if="form.dataScope === 2" label="指定部门">
                    <el-tree-select
                        v-model="form.deptIds"
                        :data="deptOptions"
                        :props="{ label: 'deptName', value: 'id', children: 'children' }"
                        multiple
                        check-strictly
                        clearable
                        style="width: 100%"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="visible = false">取消</el-button>
                <el-button type="primary" :loading="saving" :disabled="rolesLoading" @click="save">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import {
    listUserAppGrants,
    saveUserAppGrant,
    type UserAppGrant,
} from "@/api/system/app-grant";
import { listRoles } from "@/api/system/role";
import ButtonTable from "@/components/core/tables/button-table/index.vue";
import Table from "@/components/core/tables/table/index.vue";
import type { DeptNode, RoleRow } from "@/types/system";

const props = defineProps<{
    userId: string;
    deptOptions: DeptNode[];
}>();

interface GrantTableNode {
    id: string;
    kind: "application" | "detail";
    label: string;
    value: string;
    grant: UserAppGrant;
    children?: GrantTableNode[];
}

const loading = ref(false);
const saving = ref(false);
const rolesLoading = ref(false);
const visible = ref(false);
const grants = ref<UserAppGrant[]>([]);
const roleOptions = ref<RoleRow[]>([]);
let loadVersion = 0;
let roleLoadVersion = 0;
const form = reactive({
    appCode: "",
    appName: "",
    status: 0,
    roleIds: [] as string[],
    dataScope: 0 as number | null,
    deptIds: [] as string[],
});

const grantTable = computed<GrantTableNode[]>(() =>
    grants.value.map((grant) => ({
        id: grant.appCode,
        kind: "application",
        label: grant.appName,
        value: grant.appCode,
        grant,
        children: [
            {
                id: `${grant.appCode}:roles`,
                kind: "detail",
                label: "角色",
                value: grant.roleNames.length ? grant.roleNames.join("、") : "未分配",
                grant,
            },
            {
                id: `${grant.appCode}:scope`,
                kind: "detail",
                label: "数据权限",
                value: scopeLabel(grant.dataScope) ?? "跟随角色",
                grant,
            },
        ],
    })),
);

watch(() => props.userId, load, { immediate: true });
watch(visible, value => {
    if (!value) {
        roleLoadVersion += 1;
        rolesLoading.value = false;
        roleOptions.value = [];
    }
});

async function load() {
    const version = ++loadVersion;
    loading.value = true;
    try {
        const result = await listUserAppGrants(props.userId);
        if (version === loadVersion) {
            grants.value = result;
        }
    } finally {
        if (version === loadVersion) {
            loading.value = false;
        }
    }
}

async function openConfig(grant: UserAppGrant) {
    const version = ++roleLoadVersion;
    Object.assign(form, {
        appCode: grant.appCode,
        appName: grant.appName,
        status: grant.status,
        roleIds: [...grant.roleIds],
        dataScope: grant.dataScope ?? 0,
        deptIds: [...grant.deptIds],
    });
    roleOptions.value = [];
    rolesLoading.value = true;
    visible.value = true;
    try {
        const roles = await listRoles(grant.appCode);
        if (version === roleLoadVersion && form.appCode === grant.appCode) {
            roleOptions.value = roles;
        }
    } catch {
        if (version === roleLoadVersion) {
            visible.value = false;
        }
    } finally {
        if (version === roleLoadVersion) {
            rolesLoading.value = false;
        }
    }
}

async function save() {
    if (form.status === 1 && !form.roleIds.length) {
        ElMessage.warning("启用应用授权前请至少分配一个角色");
        return;
    }
    if (form.dataScope === 2 && !form.deptIds.length) {
        ElMessage.warning("请选择指定部门");
        return;
    }
    saving.value = true;
    try {
        await saveUserAppGrant(props.userId, form.appCode, {
            status: form.status,
            roleIds: [...form.roleIds],
            dataScope: form.dataScope,
            deptIds: form.dataScope === 2 ? [...form.deptIds] : [],
        });
        ElMessage.success("应用授权已保存");
        visible.value = false;
        await load();
    } finally {
        saving.value = false;
    }
}

function statusLabel(grant: UserAppGrant) {
    if (grant.appStatus !== 1) {
        return "应用已停用";
    }
    if (!grant.roleIds.length) {
        return "未分配角色";
    }
    return grant.effective ? "已启用" : "已停用";
}

function scopeLabel(scope?: number | null) {
    return scope == null || scope === 0
        ? "跟随角色"
        : ({ 1: "全部数据", 2: "指定部门", 3: "本部门", 4: "本部门及以下", 5: "仅本人" })[scope];
}
</script>

<style scoped>
.app-grant-panel {
    box-sizing: border-box;
    width: calc(100% - 80px);
    max-width: calc(100% - 80px);
    min-width: 0;
    margin-left: 80px;
    padding: 4px 0 8px;
    overflow: hidden;
}

.app-grant-table {
    width: 100%;
    max-width: 100%;
    min-width: 0;
}

.app-grant-table :deep(.art-table),
.app-grant-table :deep(.el-table) {
    width: 100%;
    max-width: 100%;
    min-width: 0;
}

.app-grant-name {
    font-weight: 500;
    color: var(--el-text-color-primary);
}

.app-grant-code,
.app-grant-value {
    color: var(--el-text-color-secondary);
}

.app-grant-detail {
    display: inline-block;
}

.app-grant-code {
    font-size: 12px;
}
</style>
