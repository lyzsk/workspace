<template>
    <div class="document-workspace">
        <div class="workspace-toolbar page-card">
            <div class="toolbar-left">
                <strong>{{ title }}</strong>
                <el-select
                    v-model="selectedId"
                    placeholder="选择文档"
                    style="width: 240px"
                    @change="selectDocument"
                >
                    <el-option
                        v-for="document in documents"
                        :key="document.id"
                        :label="document.title"
                        :value="document.id"
                    />
                </el-select>
                <el-button
                    v-permission="'system:document:create'"
                    type="primary"
                    @click="createVisible = true"
                >
                    新建文档
                </el-button>
            </div>
            <div class="toolbar-actions">
                <el-tag :type="connected ? 'success' : 'warning'">
                    {{ connected ? "在线协作中" : "离线/重连中" }}
                </el-tag>
                <el-button
                    v-if="current"
                    v-permission="'system:document:share'"
                    @click="openShare"
                >
                    共享
                </el-button>
                <el-button v-if="current" @click="importVisible = true">导入</el-button>
                <el-button v-if="current" @click="exportCurrent">导出</el-button>
            </div>
        </div>

        <div v-if="current" class="editor-shell page-card">
            <component
                :is="editorComponent"
                ref="editorRef"
                :content="current.content"
                :doc-type="docType"
                :read-only="current.permission === 'READ'"
                @operation="sendOperation"
            />
        </div>
        <el-empty v-else description="暂无文档，请新建或让其他用户共享给你" />

        <el-dialog v-model="createVisible" title="新建文档" width="420px">
            <el-input v-model.trim="newTitle" placeholder="请输入文档标题" maxlength="200" />
            <template #footer>
                <el-button @click="createVisible = false">取消</el-button>
                <el-button type="primary" @click="create">创建</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="shareVisible" title="共享文档" width="520px">
            <div class="share-form">
                <el-select v-model="shareUserId" placeholder="选择用户" style="width: 220px">
                    <el-option
                        v-for="user in users"
                        :key="user.id"
                        :label="`${user.realName} (${user.username})`"
                        :value="user.id"
                    />
                </el-select>
                <el-select v-model="sharePermission" style="width: 120px">
                    <el-option label="可编辑" value="WRITE" />
                    <el-option label="只读" value="READ" />
                </el-select>
                <el-button type="primary" @click="share">添加</el-button>
            </div>
            <el-table :data="members" size="small">
                <el-table-column prop="realName" label="用户" />
                <el-table-column prop="permission" label="权限" width="100" />
                <el-table-column label="操作" width="90">
                    <template #default="{ row }">
                        <el-button
                            v-if="row.userId !== current?.ownerId"
                            link
                            type="danger"
                            @click="removeMember(row.userId)"
                        >
                            移除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-dialog>

        <el-dialog v-model="importVisible" title="导入 JSON" width="500px">
            <el-upload
                accept=".json"
                :auto-upload="false"
                :show-file-list="true"
                :on-change="importJson"
            >
                <el-button type="primary">选择 JSON 文件</el-button>
            </el-upload>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import {
    createDocument,
    exportDocument,
    getDocument,
    getDocumentMembers,
    importDocument,
    operateDocument,
    pageDocuments,
    removeDocumentMember,
    shareDocument,
    type DocumentOperation,
    type DocumentRow,
    type DocumentType,
    type DocumentMember,
} from "@/api/system/document";
import { pageUsers, type UserRow } from "@/api/system/user";
import { DocumentSocket } from "@/utils/documentSocket";
import { useUserStore } from "@/store/modules/user";

const props = defineProps<{
    docType: DocumentType;
    title: string;
}>();

const LogicFlowEditor = defineAsyncComponent(() =>
    import("@/components/core/editors/logic-flow/index.vue"));
const ExcalidrawEditor = defineAsyncComponent(() =>
    import("@/components/core/editors/excalidraw/index.vue"));

const userStore = useUserStore();
const documents = ref<DocumentRow[]>([]);
const current = ref<DocumentRow>();
const selectedId = ref("");
const connected = ref(false);
const createVisible = ref(false);
const shareVisible = ref(false);
const importVisible = ref(false);
const newTitle = ref("");
const users = ref<UserRow[]>([]);
const members = ref<DocumentMember[]>([]);
const shareUserId = ref("");
const sharePermission = ref("WRITE");
const editorRef = ref<{ applyRemote: (operation: DocumentOperation) => void }>();
let socket: DocumentSocket | undefined;

const editorComponent = computed(() =>
    props.docType === "WHITEBOARD" ? ExcalidrawEditor : LogicFlowEditor);

async function loadDocuments() {
    const result = await pageDocuments({ pageNum: 1, pageSize: 50, docType: props.docType });
    documents.value = result.records;
    if (current.value && result.records.some((item) => item.id === current.value?.id)) {
        selectedId.value = current.value.id;
    } else if (result.records.length) {
        await selectDocument(result.records[0].id);
    } else {
        current.value = undefined;
        selectedId.value = "";
        socket?.close();
        connected.value = false;
    }
}

async function selectDocument(id: string) {
    selectedId.value = id;
    socket?.close();
    connected.value = false;
    current.value = await getDocument(id);
    await nextTick();
    socket = new DocumentSocket({
        documentId: id,
        token: userStore.token,
        onStatus: (value) => (connected.value = value),
        onOperation: (operation) => {
            if (operation.operatorId !== userStore.user?.id) {
                editorRef.value?.applyRemote(operation);
            }
            if (current.value) {
                current.value.revision = operation.revision;
            }
        },
    });
    socket.connect();
}

async function create() {
    if (!newTitle.value) {
        ElMessage.warning("请输入文档标题");
        return;
    }
    const id = await createDocument({ title: newTitle.value, docType: props.docType });
    newTitle.value = "";
    createVisible.value = false;
    await loadDocuments();
    await selectDocument(id);
}

function sendOperation(operation: DocumentOperation) {
    if (connected.value && socket) {
        socket.send(operation);
    } else if (current.value) {
        operateDocument(current.value.id, operation).then((revision) => {
            if (current.value) {
                current.value.revision = revision;
            }
        });
    }
}

async function openShare() {
    if (!current.value) {
        return;
    }
    const [memberData, userData] = await Promise.all([
        getDocumentMembers(current.value.id),
        pageUsers({ pageNum: 1, pageSize: 100 }),
    ]);
    members.value = memberData;
    users.value = userData.records;
    shareVisible.value = true;
}

async function share() {
    if (!current.value || !shareUserId.value) {
        ElMessage.warning("请选择共享用户");
        return;
    }
    await shareDocument(current.value.id, {
        userId: shareUserId.value,
        permission: sharePermission.value,
    });
    shareUserId.value = "";
    members.value = await getDocumentMembers(current.value.id);
    ElMessage.success("共享成功");
}

async function removeMember(userId: string) {
    if (!current.value) {
        return;
    }
    await removeDocumentMember(current.value.id, userId);
    members.value = await getDocumentMembers(current.value.id);
}

async function importJson(uploadFile: { raw?: File }) {
    if (!current.value || !uploadFile.raw) {
        return;
    }
    const content = JSON.parse(await uploadFile.raw.text());
    await importDocument(current.value.id, content);
    importVisible.value = false;
    ElMessage.success("导入成功");
    await selectDocument(current.value.id);
}

async function exportCurrent() {
    if (current.value) {
        await exportDocument(current.value.id, current.value.title);
    }
}

onMounted(loadDocuments);
onBeforeUnmount(() => socket?.close());
</script>

<style scoped lang="scss">
.document-workspace {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.workspace-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 12px 16px;
}

.toolbar-left,
.toolbar-actions,
.share-form {
    display: flex;
    align-items: center;
    gap: 10px;
}

.editor-shell {
    padding: 0;
    overflow: hidden;
}

.share-form {
    margin-bottom: 14px;
}
</style>
