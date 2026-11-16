<template>
    <div class="system-page">
        <SearchCard v-show="showSearch">
            <div class="search-row">
                <div class="search-field">
                    <span class="search-label">标题</span>
                    <el-input
                        v-model="query.title"
                        placeholder="请输入通知标题"
                        clearable
                        style="width: 200px"
                        @keyup.enter="search"
                    />
                </div>
                <div class="search-field">
                    <span class="search-label">状态</span>
                    <el-select
                        v-model="query.status"
                        placeholder="请选择状态"
                        clearable
                        style="width: 130px"
                    >
                        <el-option label="草稿" :value="0" />
                        <el-option label="已发布" :value="1" />
                        <el-option label="已关闭" :value="2" />
                    </el-select>
                </div>
                <div class="search-actions">
                    <el-button type="primary" @click="search">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                </div>
            </div>
        </SearchCard>

        <TableCard searchable v-model:show-search="showSearch" @refresh="load">
            <template #actions>
                <el-button
                    v-permission="'system:notice:create'"
                    type="primary"
                    @click="dialogVisible = true"
                >
                    新增通知
                </el-button>
            </template>
            <template #default="{ stripe, border, headerBackground, size }">
                <Table
                    v-loading="loading"
                    :data="rows"
                    :stripe="stripe"
                    :border="border"
                    :class="[
                        `table-size-${size}`,
                        { 'no-header-bg': !headerBackground },
                    ]"
                >
                    <el-table-column prop="title" label="标题" min-width="180" />
                    <el-table-column label="类型" width="110">
                        <template #default="{ row }">
                            {{ row.noticeType === "TODO" ? "待办" : "公告" }}
                        </template>
                    </el-table-column>
                    <el-table-column label="发布对象" min-width="150">
                        <template #default="{ row }">
                            {{ targetText(row) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" width="100">
                        <template #default="{ row }">
                            <el-tag :type="row.status === 1 ? 'success' : 'info'">
                                {{ statusText(row.status) }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="创建时间" width="170">
                        <template #default="{ row }">
                            {{ formatTime(row.createTime) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="170" align="center">
                        <template #default="{ row }">
                            <el-button
                                v-if="row.status === 0"
                                type="primary"
                                link
                                @click="openAttachment(row)"
                            >
                                附件
                            </el-button>
                            <el-button
                                v-if="row.status === 0"
                                v-permission="'system:notice:publish'"
                                type="primary"
                                link
                                @click="openPublish(row)"
                            >
                                发布
                            </el-button>
                        </template>
                    </el-table-column>
                </Table>
            </template>
            <template #pagination>
                <el-pagination
                    v-model:current-page="query.pageNum"
                    v-model:page-size="query.pageSize"
                    :total="Number(total)"
                    layout="total, sizes, prev, pager, next"
                    background
                    @current-change="load"
                    @size-change="load"
                />
            </template>
        </TableCard>

        <el-dialog v-model="dialogVisible" title="新增通知" width="620px">
            <el-form :model="form" label-width="90px">
                <el-form-item label="标题" required>
                    <el-input v-model.trim="form.title" maxlength="200" />
                </el-form-item>
                <el-form-item label="内容" required>
                    <el-input v-model="form.content" type="textarea" :rows="4" />
                </el-form-item>
                <el-form-item label="类型">
                    <el-radio-group v-model="form.noticeType">
                        <el-radio value="TODO">待办</el-radio>
                        <el-radio value="ANNOUNCEMENT">公告</el-radio>
                        <el-radio value="MESSAGE">消息</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="优先级">
                    <el-select v-model="form.priority">
                        <el-option label="普通" :value="0" />
                        <el-option label="重要" :value="1" />
                        <el-option label="紧急" :value="2" />
                    </el-select>
                </el-form-item>
                <el-form-item label="首次提醒">
                    <el-date-picker
                        v-model="form.remindTime"
                        type="datetime"
                        value-format="YYYY-MM-DDTHH:mm:ss"
                        placeholder="请选择首次提醒时间"
                        style="width: 100%"
                    />
                </el-form-item>
                <el-form-item label="重复间隔">
                    <el-input-number
                        v-model="form.repeatIntervalSeconds"
                        :min="0"
                        :step="60"
                    />
                    <span class="field-suffix">秒，0 表示不重复</span>
                </el-form-item>
                <el-form-item label="提醒次数">
                    <el-input-number v-model="form.maxRemindCount" :min="1" />
                </el-form-item>
                <el-form-item label="模板编码">
                    <el-input v-model="form.templateCode" placeholder="启用模板时填写" />
                </el-form-item>
                <el-form-item label="模板参数">
                    <el-input
                        v-model="templateParamsText"
                        type="textarea"
                        placeholder='{"team":"研发部"}'
                    />
                </el-form-item>
                <el-form-item label="发布对象">
                    <el-select v-model="form.targetType" @change="form.targetIds = []">
                        <el-option label="全员" value="ALL" />
                        <el-option label="按角色" value="ROLE" />
                        <el-option label="按部门" value="DEPT" />
                    </el-select>
                </el-form-item>
                <el-form-item v-if="form.targetType === 'ROLE'" label="角色" required>
                    <el-select v-model="form.targetIds" multiple style="width: 100%">
                        <el-option
                            v-for="role in roles"
                            :key="role.id"
                            :label="role.roleName"
                            :value="role.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item v-if="form.targetType === 'DEPT'" label="部门" required>
                    <el-select v-model="form.targetIds" multiple style="width: 100%">
                        <el-option
                            v-for="dept in depts"
                            :key="dept.id"
                            :label="dept.deptName"
                            :value="dept.id"
                        />
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="save">保存草稿</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="publishVisible" title="发布通知" width="520px">
            <el-form :model="publishForm" label-width="100px">
                <el-form-item label="业务链接">
                    <el-input
                        v-model.trim="publishForm.linkUrl"
                        placeholder="如 /system/user-center"
                    />
                </el-form-item>
                <el-form-item label="待办截止">
                    <el-date-picker
                        v-model="publishForm.dueTime"
                        type="datetime"
                        value-format="YYYY-MM-DDTHH:mm:ss"
                        placeholder="请选择截止时间"
                        style="width: 100%"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="publishVisible = false">取消</el-button>
                <el-button type="primary" :loading="publishing" @click="confirmPublish">
                    确认发布
                </el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="attachmentVisible" title="上传附件" width="460px">
            <el-upload
                accept=".txt,.pdf,.png,.jpg,.jpeg,.docx,.xlsx,.pptx"
                :auto-upload="false"
                :limit="1"
                :on-change="uploadAttachment"
            >
                <el-button type="primary" :loading="uploading">选择并上传文件</el-button>
            </el-upload>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import SearchCard from "@/components/core/tables/search-card/index.vue";
import Table from "@/components/core/tables/table/index.vue";
import TableCard from "@/components/core/tables/table-card/index.vue";
import {
    createNotice,
    pageNotices,
    publishNotice,
    uploadNoticeAttachment,
    type NoticeForm,
    type NoticeRow,
} from "@/api/system/notice";
import { getDeptList } from "@/api/system/dept";
import { listRoles } from "@/api/system/role";
import { formatTime } from "@/utils/format";
import type { DeptNode, RoleRow } from "@/types/system";
import { useFormDraft } from "@/composables/useFormDraft";

const showSearch = ref(false);
const loading = ref(false);
const saving = ref(false);
const publishing = ref(false);
const dialogVisible = ref(false);
const publishVisible = ref(false);
const attachmentVisible = ref(false);
const uploading = ref(false);
const attachmentNoticeId = ref("");
const templateParamsText = ref("");
const rows = ref<NoticeRow[]>([]);
const roles = ref<RoleRow[]>([]);
const depts = ref<DeptNode[]>([]);
const total = ref(0);
const publishingId = ref("");

const query = reactive({
    pageNum: 1,
    pageSize: 10,
    title: "",
    status: undefined as number | undefined,
});
const form = reactive<NoticeForm>({
    title: "",
    content: "",
    noticeType: "TODO",
    priority: 0,
    repeatIntervalSeconds: 0,
    maxRemindCount: 1,
    templateCode: "",
    targetType: "ALL",
    targetIds: [],
    remindTime: "",
});
const publishForm = reactive({
    linkUrl: "",
    dueTime: "",
});

const draft = useFormDraft({
    key: "notice-form",
    form,
    visible: dialogVisible,
    context: { templateParamsText },
});

const publishDraft = useFormDraft({
    key: "notice-publish",
    form: publishForm,
    visible: publishVisible,
    context: { publishingId },
});

async function load() {
    loading.value = true;
    try {
        const result = await pageNotices(query);
        rows.value = result.records;
        total.value = Number(result.total);
    } finally {
        loading.value = false;
    }
}

function search() {
    query.pageNum = 1;
    load();
}

function reset() {
    query.title = "";
    query.status = undefined;
    search();
}

async function save() {
    if (!form.title || !form.content) {
        ElMessage.warning("请填写标题和内容");
        return;
    }
    if (form.targetType !== "ALL" && form.targetIds.length === 0) {
        ElMessage.warning("请选择发布对象");
        return;
    }
    saving.value = true;
    try {
        if (templateParamsText.value.trim()) {
            try {
                form.templateParams = JSON.parse(templateParamsText.value);
            } catch {
                ElMessage.warning("模板参数必须是有效 JSON 对象");
                return;
            }
        } else {
            form.templateParams = undefined;
        }
        await createNotice(form);
        ElMessage.success("通知草稿已创建");
        draft.clear();
        dialogVisible.value = false;
        form.title = "";
        form.content = "";
        form.targetType = "ALL";
        form.targetIds = [];
        form.repeatIntervalSeconds = 0;
        form.maxRemindCount = 1;
        form.templateCode = "";
        form.remindTime = "";
        templateParamsText.value = "";
        await load();
    } finally {
        saving.value = false;
    }
}

function openAttachment(row: NoticeRow) {
    attachmentNoticeId.value = row.id;
    attachmentVisible.value = true;
}

async function uploadAttachment(upload: { raw?: File }) {
    if (!upload.raw) {
        return;
    }
    uploading.value = true;
    try {
        await uploadNoticeAttachment(attachmentNoticeId.value, upload.raw);
        ElMessage.success("附件已上传");
        attachmentVisible.value = false;
    } finally {
        uploading.value = false;
    }
}

function openPublish(row: NoticeRow) {
    publishingId.value = row.id;
    publishForm.linkUrl = "";
    publishForm.dueTime = "";
    publishVisible.value = true;
}

async function confirmPublish() {
    publishing.value = true;
    try {
        const count = await publishNotice(publishingId.value, publishForm);
        ElMessage.success(`已发布给 ${count} 个用户`);
        publishDraft.clear();
        publishVisible.value = false;
        await load();
    } finally {
        publishing.value = false;
    }
}

function targetText(row: NoticeRow) {
    if (row.targetType === "ALL") {
        return "全员";
    }
    return `${row.targetType === "ROLE" ? "角色" : "部门"} ${row.targetIds}`;
}

function statusText(status: number) {
    return status === 1 ? "已发布" : status === 2 ? "已关闭" : "草稿";
}

onMounted(() => {
    load();
    listRoles().then((data) => (roles.value = data));
    getDeptList().then((data) => (depts.value = data));
});
</script>

<style scoped>
.field-suffix {
    margin-left: 8px;
    color: var(--text-muted);
    font-size: 12px;
}
</style>