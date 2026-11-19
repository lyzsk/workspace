<template>
    <div v-loading="loading" class="app-grant-panel">
        <div v-for="grant in grants" :key="grant.appCode" class="app-grant-row">
            <div class="app-grant-name">
                <strong>{{ grant.appName }}</strong>
                <span>{{ grant.appCode }}</span>
            </div>
            <el-tag :type="grant.effective ? 'success' : 'info'">
                {{ grant.appStatus !== 1 ? "应用已停用" : !grant.roleIds.length ? "未分配角色" : grant.effective ? "已启用" : "已停用" }}
            </el-tag>
            <div class="app-grant-value" :title="grant.roleNames.join('、')">
                角色：{{ grant.roleNames.length ? grant.roleNames.join("、") : "未分配" }}
            </div>
            <div class="app-grant-value">数据权限：{{ scopeLabel(grant.dataScope) }}</div>
            <el-button
                v-permission="'sys:user:app-grant'"
                size="small"
                @click="openConfig(grant)"
            >
                配置
            </el-button>
        </div>
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
                        <el-option label="跟随应用角色" :value="null" />
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
import { reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import {
    listUserAppGrants,
    saveUserAppGrant,
    type UserAppGrant,
} from "@/api/system/app-grant";
import { listRoles } from "@/api/system/role";
import type { DeptNode, RoleRow } from "@/types/system";

const props = defineProps<{
    userId: string;
    deptOptions: DeptNode[];
}>();

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
    dataScope: null as number | null,
    deptIds: [] as string[],
});

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
        dataScope: grant.dataScope ?? null,
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

function scopeLabel(scope?: number | null) {
    return scope == null
        ? "跟随角色"
        : ({ 1: "全部数据", 2: "指定部门", 3: "本部门", 4: "本部门及以下", 5: "仅本人" })[scope];
}
</script>

<style scoped>
.app-grant-panel {
    padding: 8px 24px;
}

.app-grant-row {
    display: grid;
    grid-template-columns: minmax(180px, 1.2fr) 90px minmax(180px, 1.5fr) 180px 72px;
    gap: 16px;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid var(--el-border-color-lighter);
}

.app-grant-name {
    display: flex;
    gap: 8px;
    align-items: baseline;
}

.app-grant-name span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
}

.app-grant-value {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

@media (max-width: 900px) {
    .app-grant-row {
        grid-template-columns: 1fr;
        gap: 8px;
    }

    .app-grant-value {
        white-space: normal;
    }
}
</style>
