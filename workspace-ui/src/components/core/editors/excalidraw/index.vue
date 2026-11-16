<template>
    <div ref="host" class="excalidraw-host"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import React from "react";
import { createRoot, type Root } from "react-dom/client";
import { Excalidraw } from "@excalidraw/excalidraw";
import "@excalidraw/excalidraw/index.css";
import type { ExcalidrawImperativeAPI } from "@excalidraw/excalidraw/types";
import type { ExcalidrawElement } from "@excalidraw/excalidraw/element/types";
import type { DocumentOperation } from "@/api/system/document";
import { useAppStore } from "@/store/modules/app";

const props = defineProps<{
    content: string;
    readOnly: boolean;
}>();
const emit = defineEmits<{
    operation: [operation: DocumentOperation];
}>();
const host = ref<HTMLDivElement>();
const appStore = useAppStore();
let root: Root | undefined;
let api: ExcalidrawImperativeAPI | undefined;
let applyingRemote = false;
let previous = new Map<string, any>();

function theme() {
    return appStore.isDark ? "dark" : "light";
}

function render() {
    const content = JSON.parse(props.content) as {
        elements?: Record<string, unknown>;
        appState?: Record<string, unknown>;
    };
    root?.render(React.createElement(Excalidraw, {
        initialData: {
            elements: Object.values(content.elements || {}) as ExcalidrawElement[],
            appState: { ...content.appState, theme: theme() },
        },
        theme: theme(),
        viewModeEnabled: props.readOnly,
        excalidrawAPI: (value: ExcalidrawImperativeAPI) => {
            api = value;
            previous = new Map(value.getSceneElements().map((item: ExcalidrawElement) => [item.id, item]));
        },
        onChange: (elements: readonly ExcalidrawElement[]) => {
            if (applyingRemote) {
                return;
            }
            const current = new Map(elements.map((item) => [item.id, item]));
            for (const [id, payload] of current) {
                if (JSON.stringify(previous.get(id)) !== JSON.stringify(payload)) {
                    emit("operation", {
                        opType: "UPSERT",
                        targetType: "element",
                        targetId: id,
                        payload,
                    });
                }
            }
            for (const id of previous.keys()) {
                if (!current.has(id)) {
                    emit("operation", {
                        opType: "DELETE",
                        targetType: "element",
                        targetId: id,
                    });
                }
            }
            previous = current;
        },
    }));
}

function applyRemote(operation: DocumentOperation) {
    if (!api) {
        return;
    }
    applyingRemote = true;
    try {
        if (operation.opType === "SCENE_REPLACE") {
            const content = operation.payload as { elements?: Record<string, unknown> };
            previous = new Map(Object.entries(content.elements || {}));
        } else if (operation.opType === "DELETE") {
            previous.delete(operation.targetId!);
        } else {
            previous.set(operation.targetId!, operation.payload);
        }
        api.updateScene({ elements: Array.from(previous.values()) });
    } finally {
        setTimeout(() => {
            applyingRemote = false;
        }, 0);
    }
}

defineExpose({ applyRemote });

onMounted(() => {
    root = createRoot(host.value!);
    render();
});

watch(() => appStore.isDark, render);

onBeforeUnmount(() => {
    root?.unmount();
    root = undefined;
    api = undefined;
});
</script>

<style scoped lang="scss">
.excalidraw-host {
    width: 100%;
    height: calc(100vh - 230px);
    min-height: 540px;
    border: 1px solid var(--border);
    border-radius: 8px;
    overflow: hidden;
}

.excalidraw-host :deep(.excalidraw) {
    --ui-font: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}
</style>
