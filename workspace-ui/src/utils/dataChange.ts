import { ref } from "vue";
import { ElMessage } from "element-plus";
import { SessionConfig } from "@/config/session";
import { hasDirtyDrafts } from "@/utils/draft";
import { isExpired } from "@/utils/session";

export const dataRefreshKey = ref(0);

const PERMISSION_CHANGED_PREFIXES = [
    "/api/system/roles",
    "/api/system/menus",
    "/api/system/users",
];

let pending = false;
let permissionChanged = false;
let timer: number | undefined;
let lastHintAt = 0;

function scheduleRefresh() {
    window.clearTimeout(timer);
    timer = window.setTimeout(() => {
        const shouldReloadPermissions = permissionChanged;
        permissionChanged = false;
        window.dispatchEvent(
            new CustomEvent("workspace:data-refresh", {
                detail: { permissionChanged: shouldReloadPermissions },
            }),
        );
    }, SessionConfig.dataRefreshDebounceMs);
}

export function notifyDataChanged(path?: string) {
    if (path && PERMISSION_CHANGED_PREFIXES.some((prefix) => path.startsWith(prefix))) {
        permissionChanged = true;
    }
    if (pending || isExpired()) {
        return;
    }
    if (document.hidden) {
        pending = true;
        return;
    }
    if (hasDirtyDrafts()) {
        pending = true;
        const now = Date.now();
        if (now - lastHintAt > 5000) {
            lastHintAt = now;
            ElMessage.info("数据已更新，当前编辑完成或取消后将自动刷新");
        }
        return;
    }
    scheduleRefresh();
}

export function applyPendingDataChange() {
    if (!pending || document.hidden || hasDirtyDrafts() || isExpired()) {
        return;
    }
    pending = false;
    scheduleRefresh();
}

export function resetDataRefreshKey() {
    dataRefreshKey.value = 0;
    pending = false;
    permissionChanged = false;
    window.clearTimeout(timer);
}

window.addEventListener("workspace:draft-settled", applyPendingDataChange);
