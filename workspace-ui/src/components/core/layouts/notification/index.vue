<template>
    <div
        v-show="visible"
        class="notification-panel"
        :class="{ 'is-show': show }"
        @click.stop
    >
        <div class="panel-header">
            <span class="panel-title">通知中心</span>
            <div class="panel-actions">
                <button class="panel-action" @click="markAllRead">全部已读</button>
                <button
                    class="panel-action"
                    title="免打扰设置"
                    @click="settingsVisible = true"
                >
                    <SvgIcon icon="ri:settings-3-line" />
                </button>
            </div>
        </div>

        <div class="notification-tabs">
            <button
                v-for="(tab, index) in tabs"
                :key="tab.key"
                :class="{ active: activeTab === index }"
                @click="activeTab = index"
            >
                {{ tab.name }} ({{ tab.items.length }})
            </button>
        </div>

        <div class="notification-list">
            <template v-if="currentItems.length">
                <div
                    v-for="item in currentItems"
                    :key="item.id"
                    class="notification-item"
                    :class="{ unread: item.readFlag === 0 }"
                    @click="openItem(item)"
                >
                    <div class="item-icon" :class="iconClass(item)">
                        <SvgIcon :icon="iconOf(item)" />
                    </div>
                    <div class="item-content">
                        <div class="item-title">{{ item.title }}</div>
                        <div class="item-time">{{ formatTime(item.createTime) }}</div>
                        <div v-if="item.dueTime" class="item-due">
                            截止 {{ formatTime(item.dueTime) }}
                        </div>
                        <div v-if="expandedId === item.id" class="item-detail">
                            <p>{{ item.content }}</p>
                            <div
                                v-for="file in attachments"
                                :key="file.id"
                                class="attachment-link"
                                @click.stop="downloadAttachment(item.noticeId, file.id, file.originalName)"
                            >
                                <SvgIcon icon="ri:attachment-2" />
                                {{ file.originalName }}
                            </div>
                            <el-button
                                v-if="item.linkUrl"
                                size="small"
                                link
                                type="primary"
                                @click.stop="openLink(item.linkUrl)"
                            >
                                打开业务页面
                            </el-button>
                        </div>
                    </div>
                    <el-button
                        v-if="item.category === 'TODO' && item.doneFlag === 0"
                        size="small"
                        link
                        type="primary"
                        @click.stop="markDone(item.id)"
                    >
                        完成
                    </el-button>
                </div>
            </template>
            <div v-else class="notification-empty">
                <SvgIcon icon="ri:inbox-line" />
                <p>暂无{{ tabs[activeTab].name }}</p>
            </div>
        </div>

        <div class="panel-footer">
            <el-button class="view-all" @click="viewAll">查看全部</el-button>
        </div>
    </div>

    <el-dialog v-model="settingsVisible" title="免打扰设置" width="420px">
        <el-form label-width="100px">
            <el-form-item label="启用免打扰">
                <el-switch
                    v-model="preference.quietEnabled"
                    :active-value="1"
                    :inactive-value="0"
                />
            </el-form-item>
            <el-form-item label="开始时间">
                <el-time-picker
                    v-model="preference.quietStart"
                    value-format="HH:mm:ss"
                    placeholder="如 22:00:00"
                    style="width: 100%"
                />
            </el-form-item>
            <el-form-item label="结束时间">
                <el-time-picker
                    v-model="preference.quietEnd"
                    value-format="HH:mm:ss"
                    placeholder="如 06:00:00"
                    style="width: 100%"
                />
            </el-form-item>
            <el-form-item label="时区">
                <el-input v-model="preference.timezone" />
            </el-form-item>
        </el-form>
        <template #footer>
            <el-button @click="settingsVisible = false">取消</el-button>
            <el-button type="primary" @click="savePreference">保存</el-button>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { storeToRefs } from "pinia";
import SvgIcon from "@/components/core/base/svg-icon/index.vue";
import { useNotificationStore } from "@/store/modules/notification";
import { formatTime } from "@/utils/format";
import type { MessageRow } from "@/api/system/message";
import {
    getNotificationPreference,
    updateNotificationPreference,
} from "@/api/system/notification-preference";
import {
    downloadNoticeAttachment,
    listNoticeAttachments,
    type NoticeAttachment,
} from "@/api/system/attachment";
import { useFormDraft } from "@/composables/useFormDraft";

defineOptions({ name: "Notification" });

const props = defineProps<{
    modelValue: boolean;
}>();

const emit = defineEmits<{
    "update:modelValue": [value: boolean];
}>();

const router = useRouter();
const store = useNotificationStore();
const { messages } = storeToRefs(store);
const { markRead, markAllRead, markDone, start, stop } = store;
const visible = ref(false);
const show = ref(false);
const settingsVisible = ref(false);
const activeTab = ref(0);
const expandedId = ref("");
const attachments = ref<NoticeAttachment[]>([]);
const preference = reactive({
    quietEnabled: 0,
    quietStart: "",
    quietEnd: "",
    timezone: "Asia/Shanghai",
});

const preferenceDraft = useFormDraft({
    key: "notification-preference",
    form: preference,
    visible: settingsVisible,
    autoRestore: false,
});

const notices = computed(() =>
    messages.value.filter(
        (item) => item.category === "ANNOUNCEMENT" || item.category === "SYSTEM",
    ),
);
const directMessages = computed(() =>
    messages.value.filter((item) => item.category === "MESSAGE"),
);
const todos = computed(() =>
    messages.value.filter(
        (item) => item.category === "TODO" && item.doneFlag === 0,
    ),
);

const tabs = computed(() => [
    { key: "notice", name: "通知", items: notices.value },
    { key: "message", name: "消息", items: directMessages.value },
    { key: "todo", name: "待办", items: todos.value },
]);
const currentItems = computed(() => tabs.value[activeTab.value].items);

watch(
    () => props.modelValue,
    (open) => {
        if (open) {
            visible.value = true;
            setTimeout(() => {
                show.value = true;
            }, 5);
        } else {
            show.value = false;
            setTimeout(() => {
                visible.value = false;
            }, 350);
        }
    },
    { immediate: true },
);

function iconOf(item: MessageRow) {
    if (item.category === "TODO") {
        return "ri:task-line";
    }
    return item.category === "MESSAGE" ? "ri:message-3-line" : "ri:notification-3-line";
}

function iconClass(item: MessageRow) {
    if (item.category === "TODO") {
        return "todo-icon";
    }
    return item.category === "MESSAGE" ? "message-icon" : "notice-icon";
}

async function openItem(item: MessageRow) {
    if (expandedId.value === item.id) {
        expandedId.value = "";
        attachments.value = [];
        return;
    }
    expandedId.value = item.id;
    if (item.readFlag === 0) {
        await markRead(item.id);
    }
    attachments.value = await listNoticeAttachments(item.noticeId);
}

function openLink(linkUrl: string) {
    emit("update:modelValue", false);
    router.push(linkUrl);
}

function viewAll() {
    emit("update:modelValue", false);
}

async function downloadAttachment(noticeId: string, fileId: string, filename: string) {
    await downloadNoticeAttachment(noticeId, fileId, filename);
}

async function loadPreference() {
    const data = await getNotificationPreference();
    preference.quietEnabled = data.quietEnabled;
    preference.quietStart = data.quietStart || "";
    preference.quietEnd = data.quietEnd || "";
    preference.timezone = data.timezone;
}

async function savePreference() {
    if (preference.quietEnabled === 1 && (!preference.quietStart || !preference.quietEnd)) {
        ElMessage.warning("请填写免打扰起止时间");
        return;
    }
    await updateNotificationPreference(preference);
    preferenceDraft.clear();
    settingsVisible.value = false;
    ElMessage.success("免打扰设置已保存");
}

onMounted(() => {
    start();
    loadPreference().then(() => {
        if (!preferenceDraft.restore()) {
            preferenceDraft.markClean();
        }
    });
});
onUnmounted(stop);
</script>

<style scoped lang="scss">
.notification-panel {
    position: fixed;
    top: 70px;
    right: 20px;
    z-index: 2000;
    display: flex;
    flex-direction: column;
    width: 360px;
    height: 500px;
    overflow: hidden;
    border: 1px solid var(--border);
    border-radius: 12px;
    background: var(--panel-bg);
    box-shadow: 0 12px 32px rgb(0 0 0 / 18%);
    color: var(--text);
    opacity: 0;
    transform: scaleY(0.9);
    transform-origin: top;
    transition:
        transform 0.3s,
        opacity 0.3s;
}

.notification-panel.is-show {
    opacity: 1;
    transform: scaleY(1);
}

.panel-header {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: space-between;
    padding: 14px 14px 10px;
}

.panel-title {
    font-size: 15px;
    font-weight: 600;
}

.panel-actions {
    display: flex;
    align-items: center;
    gap: 4px;
}

.panel-action {
    display: flex;
    align-items: center;
    padding: 4px 6px;
    border: 0;
    border-radius: 6px;
    background: transparent;
    color: var(--text-muted);
    font-size: 12px;
    cursor: pointer;
}

.panel-action:hover {
    background: var(--slider-bg);
    color: var(--text);
}

.panel-action .art-svg-icon {
    width: 16px;
    height: 16px;
}

.notification-tabs {
    display: flex;
    flex-shrink: 0;
    gap: 20px;
    height: 50px;
    padding: 0 14px;
    border-bottom: 1px solid var(--border);
}

.notification-tabs button {
    padding: 0;
    border: 0;
    border-bottom: 2px solid transparent;
    background: transparent;
    color: var(--text-muted);
    font-size: 13px;
    cursor: pointer;
}

.notification-tabs button.active {
    border-bottom-color: var(--primary);
    color: var(--primary);
}

.notification-list {
    flex: 1;
    overflow-y: auto;
}

.notification-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 14px;
    cursor: pointer;
}

.notification-item:hover {
    background: var(--slider-bg);
}

.notification-item.unread .item-title {
    font-weight: 600;
}

.notification-item.unread .item-title::after {
    display: inline-block;
    width: 5px;
    height: 5px;
    margin-left: 6px;
    border-radius: 50%;
    background: var(--primary);
    content: "";
    vertical-align: middle;
}

.item-icon {
    display: flex;
    flex: 0 0 36px;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: 9px;
    font-size: 18px;
}

.notice-icon {
    background: rgb(93 135 255 / 12%);
    color: var(--primary);
}

.message-icon {
    background: rgb(103 194 58 / 12%);
    color: var(--el-color-success);
}

.todo-icon {
    background: rgb(240 173 78 / 14%);
    color: var(--el-color-warning);
}

.item-content {
    flex: 1;
    min-width: 0;
}

.item-title {
    overflow: hidden;
    font-size: 13px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.item-time,
.item-due {
    margin-top: 4px;
    color: var(--text-muted);
    font-size: 11px;
}

.item-detail {
    padding-top: 8px;
    color: var(--text-muted);
    font-size: 12px;
    white-space: normal;
}

.item-detail p {
    margin: 0 0 8px;
    white-space: pre-wrap;
}

.attachment-link {
    display: flex;
    align-items: center;
    gap: 5px;
    margin-top: 5px;
    color: var(--primary);
    cursor: pointer;
}

.notification-empty {
    padding-top: 90px;
    color: var(--text-muted);
    text-align: center;
}

.notification-empty svg {
    width: 42px;
    height: 42px;
}

.notification-empty p {
    margin-top: 12px;
    font-size: 12px;
}

.panel-footer {
    flex-shrink: 0;
    padding: 12px 14px;
    border-top: 1px solid var(--border);
}

.view-all {
    width: 100%;
}
</style>
