import { onMounted, onUnmounted } from "vue";
import { ElNotification, type NotificationHandle } from "element-plus";
import { logout as logoutApi } from "@/api/auth";
import { SessionConfig } from "@/config/session";
import { isExpired, handleSessionExpired } from "@/utils/session";

const ACTIVITY_KEY = "workspace:lastActivity";
const CHANNEL_NAME = "workspace-session";

function readLastActivity() {
    const value = Number(localStorage.getItem(ACTIVITY_KEY) ?? 0);
    return Number.isFinite(value) ? value : 0;
}

export function useIdleLogout() {
    let checkTimer: number | undefined;
    let warning: NotificationHandle | undefined;
    let lastWrite = 0;
    let channel: BroadcastChannel | undefined;
    let loggingOut = false;

    function markActivity() {
        const now = Date.now();
        if (now - lastWrite < 1000) {
            return;
        }
        lastWrite = now;
        localStorage.setItem(ACTIVITY_KEY, String(now));
        if (warning) {
            warning.close();
            warning = undefined;
        }
    }

    async function forceLogout() {
        if (loggingOut || isExpired()) {
            return;
        }
        loggingOut = true;
        warning?.close();
        warning = undefined;
        channel?.postMessage({ type: "logout", reason: "idle" });
        await logoutApi({ silent: true }).catch(() => undefined);
        handleSessionExpired("idle");
    }

    function check() {
        const idle = Date.now() - readLastActivity();
        if (idle >= SessionConfig.idleTimeoutMs) {
            forceLogout();
            return;
        }
        if (idle >= SessionConfig.idleTimeoutMs - SessionConfig.idleWarningMs && !warning) {
            warning = ElNotification({
                title: "即将自动退出",
                message: "长时间未操作，1 分钟后将退出登录",
                type: "warning",
                duration: 0,
            });
        }
    }

    function onStorageActivity() {
        if (warning && Date.now() - readLastActivity() < SessionConfig.idleTimeoutMs - SessionConfig.idleWarningMs) {
            warning.close();
            warning = undefined;
        }
    }

    onMounted(() => {
        const now = Date.now();
        if (!readLastActivity() || now - readLastActivity() > SessionConfig.idleTimeoutMs) {
            localStorage.setItem(ACTIVITY_KEY, String(now));
        }
        window.addEventListener("mousemove", markActivity, { passive: true });
        window.addEventListener("mousedown", markActivity, { passive: true });
        window.addEventListener("keydown", markActivity);
        window.addEventListener("wheel", markActivity, { passive: true });
        window.addEventListener("touchstart", markActivity, { passive: true });
        document.addEventListener("scroll", markActivity, { capture: true, passive: true });
        window.addEventListener("storage", onStorageActivity);

        if (typeof BroadcastChannel !== "undefined") {
            channel = new BroadcastChannel(CHANNEL_NAME);
            channel.onmessage = (event) => {
                if (event.data?.type === "logout" && !loggingOut) {
                    loggingOut = true;
                    handleSessionExpired(event.data.reason === "idle" ? "idle" : "expired");
                }
            };
        }

        checkTimer = window.setInterval(check, SessionConfig.idleCheckIntervalMs);
    });

    onUnmounted(() => {
        window.removeEventListener("mousemove", markActivity);
        window.removeEventListener("mousedown", markActivity);
        window.removeEventListener("keydown", markActivity);
        window.removeEventListener("wheel", markActivity);
        window.removeEventListener("touchstart", markActivity);
        document.removeEventListener("scroll", markActivity, { capture: true });
        window.removeEventListener("storage", onStorageActivity);
        window.clearInterval(checkTimer);
        warning?.close();
        channel?.close();
    });
}
