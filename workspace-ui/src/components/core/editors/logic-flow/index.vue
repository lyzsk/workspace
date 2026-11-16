<template>
    <div class="logic-flow-editor">
        <div ref="container" class="logic-flow-container"></div>
    </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import LogicFlow from "@logicflow/core";
import { BpmnElement, DndPanel, MiniMap } from "@logicflow/extension";
import "@logicflow/core/dist/index.css";
import "@logicflow/extension/dist/index.css";
import UmlNode from "./umlNode";
import type { DocumentOperation, DocumentType } from "@/api/system/document";

const props = defineProps<{
    content: string;
    docType: DocumentType;
    readOnly: boolean;
}>();
const emit = defineEmits<{
    operation: [operation: DocumentOperation];
}>();
const container = ref<HTMLDivElement>();
interface GraphItem {
    id: string;
    [key: string]: unknown;
}
let lf: LogicFlow | undefined;
let applyingRemote = false;
let diffTimer: ReturnType<typeof setTimeout> | undefined;
let previous = { elements: new Map<string, unknown>(), relations: new Map<string, unknown>() };

function snapshot() {
    if (!lf) {
        return previous;
    }
    const data = lf.getGraphData() as unknown as {
        nodes: GraphItem[];
        edges: GraphItem[];
    };
    return {
        elements: new Map<string, unknown>(
            (data.nodes || []).map((item) => [item.id, item] as [string, unknown]),
        ),
        relations: new Map<string, unknown>(
            (data.edges || []).map((item) => [item.id, item] as [string, unknown]),
        ),
    };
}

function diff() {
    if (applyingRemote || !lf) {
        return;
    }
    const current = snapshot();
    emitMapDiff(previous.elements, current.elements, "element");
    emitMapDiff(previous.relations, current.relations, "relation");
    previous = current;
}

function emitMapDiff(
    before: Map<string, unknown>,
    current: Map<string, unknown>,
    targetType: "element" | "relation",
) {
    for (const [id, payload] of current) {
        if (JSON.stringify(before.get(id)) !== JSON.stringify(payload)) {
            emit("operation", { opType: "UPSERT", targetType, targetId: id, payload });
        }
    }
    for (const id of before.keys()) {
        if (!current.has(id)) {
            emit("operation", { opType: "DELETE", targetType, targetId: id });
        }
    }
}

function scheduleDiff() {
    clearTimeout(diffTimer);
    diffTimer = setTimeout(diff, 80);
}

function patternItems() {
    if (props.docType === "FLOW") {
        return [
            { type: "bpmn:startEvent", label: "开始", text: "开始" },
            { type: "bpmn:endEvent", label: "结束", text: "结束" },
            { type: "bpmn:userTask", label: "用户任务", text: "用户任务" },
            { type: "bpmn:serviceTask", label: "服务任务", text: "服务任务" },
            { type: "bpmn:exclusiveGateway", label: "网关", text: "网关" },
        ];
    }
    if (props.docType === "UML") {
        return [{ type: "uml", label: "类", text: "Class" }];
    }
    return [
        { type: "rect", label: "主题", text: "主题" },
        { type: "diamond", label: "子主题", text: "子主题" },
    ];
}

function applyRemote(operation: DocumentOperation) {
    if (!lf || applyingRemote) {
        return;
    }
    applyingRemote = true;
    try {
        if (operation.opType === "SCENE_REPLACE") {
            const content = operation.payload as {
                elements?: Record<string, unknown>;
                relations?: Record<string, unknown>;
            };
            lf.render({
                nodes: Object.values(content.elements || {}) as never,
                edges: Object.values(content.relations || {}) as never,
            });
        } else if (operation.targetType === "element") {
            if (operation.opType === "DELETE") {
                lf.deleteNode(operation.targetId!);
            } else {
                lf.deleteNode(operation.targetId!);
                lf.addNode(operation.payload as never);
            }
        } else if (operation.targetType === "relation") {
            if (operation.opType === "DELETE") {
                lf.deleteEdge(operation.targetId!);
            } else {
                lf.deleteEdge(operation.targetId!);
                lf.addEdge(operation.payload as never);
            }
        }
        previous = snapshot();
    } finally {
        applyingRemote = false;
    }
}

defineExpose({ applyRemote });

onMounted(async () => {
    const plugins = props.docType === "FLOW"
        ? [DndPanel, MiniMap, BpmnElement]
        : [DndPanel, MiniMap];
    lf = new LogicFlow({
        container: container.value!,
        grid: true,
        keyboard: { enabled: !props.readOnly },
        plugins,
    });
    if (props.docType === "UML") {
        lf.register(UmlNode);
    }
    lf.setPatternItems(patternItems());
    lf.updateEditConfig({ isSilentMode: props.readOnly });
    const content = JSON.parse(props.content) as {
        elements?: Record<string, unknown>;
        relations?: Record<string, unknown>;
    };
    lf.render({
        nodes: Object.values(content.elements || {}) as never,
        edges: Object.values(content.relations || {}) as never,
    });
    previous = snapshot();
    if (props.docType === "MIND" && previous.elements.size === 0 && !props.readOnly) {
        lf.addNode({ type: "rect", x: 500, y: 300, text: "中心主题" });
        await nextTick();
        previous = snapshot();
        emit("operation", {
            opType: "UPSERT",
            targetType: "element",
            targetId: (lf.getGraphData() as unknown as { nodes: GraphItem[] }).nodes[0].id,
            payload: (lf.getGraphData() as unknown as { nodes: GraphItem[] }).nodes[0],
        });
    }
    lf.on("history:change", scheduleDiff);
});

onBeforeUnmount(() => {
    clearTimeout(diffTimer);
    lf?.destroy();
    lf = undefined;
});
</script>

<style scoped lang="scss">
.logic-flow-editor,
.logic-flow-container {
    width: 100%;
    height: calc(100vh - 230px);
    min-height: 540px;
    background: var(--app-bg);
}

.logic-flow-container :deep(.lf-dndpanel) {
    top: 12px;
    left: 12px;
    padding: 8px;
    border: 1px solid var(--border);
    border-radius: 8px;
    background: var(--panel-bg);
}

.logic-flow-container :deep(.lf-dnd-item) {
    margin-bottom: 6px;
    color: var(--text);
}

.logic-flow-container :deep(.uml-class-node) {
    overflow: hidden;
    border: 1px solid var(--primary);
    border-radius: 4px;
    background: var(--panel-bg);
    color: var(--text);
    font-size: 12px;
}

.logic-flow-container :deep(.uml-class-head) {
    padding: 8px;
    background: var(--primary);
    color: #fff;
    font-weight: 600;
    text-align: center;
}

.logic-flow-container :deep(.uml-class-section) {
    min-height: 34px;
    padding: 7px;
    border-top: 1px solid var(--border);
    white-space: pre-wrap;
}
</style>
