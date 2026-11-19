import { onMounted, onUnmounted } from "vue";
import { ElNotification, type NotificationHandle } from "element-plus";
import { SessionConfig } from "@/config/session";
import { useUserStore } from "@/store/modules/user";

const ACTIVITY_KEY = "workspace:lastActivity";

function readLastActivity() {
    const value = Number(localStorage.getItem(ACTIVITY_KEY) ?? 0);
    return Number.isFinite(value) ? value : 0;
}

export function useIdleLogout() {
    const userStore = useUserStore();
    let checkTimer: number | undefined;
    let warning: NotificationHandle | undefined;
    let lastWrite = 0;
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
        if (loggingOut || userStore.status === "expired") {
            return;
        }
        loggingOut = true;
        warning?.close();
        warning = undefined;
        await userStore.logout("idle");
    }

    function check() {
        const idle = Date.now() - readLastActivity();
        if (idle >= SessionConfig.idleTimeoutMs) {
            void forceLogout();
            return;
        }
        if (
            idle >=
                SessionConfig.idleTimeoutMs - SessionConfig.idleWarningMs &&
            !warning
        ) {
            warning = ElNotification({
                title: "即将自动退出",
                message: "长时间未操作，1 分钟后将退出登录",
                type: "warning",
                duration: 0,
            });
        }
    }

    onMounted(() => {
        const now = Date.now();
        if (
            !readLastActivity() ||
            now - readLastActivity() > SessionConfig.idleTimeoutMs
        ) {
            localStorage.setItem(ACTIVITY_KEY, String(now));
        }
        window.addEventListener("mousemove", markActivity, { passive: true });
        window.addEventListener("mousedown", markActivity, { passive: true });
        window.addEventListener("keydown", markActivity);
        window.addEventListener("wheel", markActivity, { passive: true });
        window.addEventListener("touchstart", markActivity, { passive: true });
        document.addEventListener("scroll", markActivity, {
            capture: true,
            passive: true,
        });
        checkTimer = window.setInterval(
            check,
            SessionConfig.idleCheckIntervalMs,
        );
    });

    onUnmounted(() => {
        window.removeEventListener("mousemove", markActivity);
        window.removeEventListener("mousedown", markActivity);
        window.removeEventListener("keydown", markActivity);
        window.removeEventListener("wheel", markActivity);
        window.removeEventListener("touchstart", markActivity);
        document.removeEventListener("scroll", markActivity, { capture: true });
        window.clearInterval(checkTimer);
        warning?.close();
    });
}
