import { nextTick, onMounted, onUnmounted, ref, watch, type Ref } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { SessionConfig } from "@/config/session";
import { useUserStore } from "@/store/modules/user";
import {
    buildDraftKey,
    clearDraft,
    isSessionEnding,
    notifyDraftSettled,
    readDraft,
    registerDraft,
    saveDraft,
    type DraftPayload,
} from "@/utils/draft";

const SENSITIVE_KEYS = [
    "password",
    "oldPassword",
    "currentPassword",
    "newPassword",
    "confirmPassword",
    "secret",
    "token",
];

interface FormDraftOptions<T extends object> {
    key: string;
    form: T;
    visible?: Ref<boolean>;
    context?: Record<string, Ref<unknown>>;
    omit?: string[];
    autoRestore?: boolean;
}

export function useFormDraft<T extends object>(options: FormDraftOptions<T>) {
    const route = useRoute();
    const userStore = useUserStore();
    const draftKey = buildDraftKey(userStore.user?.id, route.path, options.key);
    const omitted = new Set([...SENSITIVE_KEYS, ...(options.omit ?? [])]);
    const dirty = ref(false);
    const restored = ref(false);

    let baseline = "";
    let saveTimer: number | undefined;
    let unregister: (() => void) | undefined;

    function clone(source: object) {
        const result: Record<string, unknown> = {};
        for (const [key, value] of Object.entries(source as Record<string, unknown>)) {
            if (!omitted.has(key)) {
                result[key] = value;
            }
        }
        return JSON.parse(JSON.stringify(result)) as Record<string, unknown>;
    }

    function contextSnapshot() {
        if (!options.context) {
            return undefined;
        }
        const result: Record<string, unknown> = {};
        for (const [key, value] of Object.entries(options.context)) {
            result[key] =
                value.value === undefined
                    ? null
                    : JSON.parse(JSON.stringify(value.value));
        }
        return result;
    }

    function payload(): DraftPayload {
        return {
            version: 1,
            savedAt: Date.now(),
            form: clone(options.form) as Record<string, unknown>,
            context: contextSnapshot(),
            visible: options.visible?.value,
        };
    }

    function refreshDirty() {
        dirty.value = JSON.stringify(clone(options.form)) !== baseline;
    }

    function persist() {
        if (!dirty.value || options.visible?.value === false) {
            return;
        }
        saveDraft(draftKey, payload());
    }

    function scheduleSave() {
        window.clearTimeout(saveTimer);
        saveTimer = window.setTimeout(persist, SessionConfig.draftAutosaveDelayMs);
    }

    function restore() {
        const saved = readDraft(draftKey);
        if (!saved) {
            return false;
        }
        Object.assign(options.form, saved.form);
        if (options.context && saved.context) {
            for (const [key, value] of Object.entries(saved.context)) {
                const target = options.context[key];
                if (target) {
                    target.value = value;
                }
            }
        }
        if (options.visible) {
            options.visible.value = saved.visible ?? true;
        }
        baseline = "";
        dirty.value = true;
        restored.value = true;
        ElMessage.info("已恢复上次未提交的内容");
        return true;
    }

    function clear() {
        clearDraft(draftKey);
        baseline = JSON.stringify(clone(options.form));
        dirty.value = false;
        restored.value = false;
        notifyDraftSettled();
    }

    function markClean() {
        baseline = JSON.stringify(clone(options.form));
        dirty.value = false;
        restored.value = false;
    }

    watch(
        () => [options.form, options.context],
        () => {
            refreshDirty();
            if (dirty.value) {
                scheduleSave();
            }
        },
        { deep: true },
    );

    if (options.context) {
        watch(
            () => Object.values(options.context!).map((item) => item.value),
            () => {
                refreshDirty();
                if (dirty.value) {
                    scheduleSave();
                }
            },
        );
    }

    if (options.visible) {
        watch(options.visible, (visible) => {
            if (visible) {
                nextTick(() => {
                    baseline = JSON.stringify(clone(options.form));
                    refreshDirty();
                });
                return;
            }
            if (!isSessionEnding()) {
                clear();
            }
        });
    }

    onMounted(() => {
        baseline = JSON.stringify(clone(options.form));
        if (options.autoRestore !== false) {
            restore();
        }
        unregister = registerDraft({
            key: draftKey,
            snapshot: () => ({ payload: payload(), dirty: dirty.value }),
        });
    });

    onUnmounted(() => {
        persist();
        unregister?.();
        notifyDraftSettled();
    });

    return {
        dirty,
        restored,
        restore,
        clear,
        markClean,
        discard: clear,
    };
}
