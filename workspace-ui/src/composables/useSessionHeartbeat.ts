import { onMounted, onUnmounted } from "vue";
import { touch } from "@/api/auth";
import { SessionConfig } from "@/config/session";
import { useUserStore } from "@/store/modules/user";
import { isExpired } from "@/utils/session";

const ACTIVITY_EVENTS = [
    "mousemove",
    "mousedown",
    "keydown",
    "wheel",
    "touchstart",
    "scroll",
] as const;

export function useSessionHeartbeat() {
    const userStore = useUserStore();
    let lastTouchAt = 0;
    let touching = false;

    async function onActivity() {
        const now = Date.now();
        if (
            touching ||
            isExpired() ||
            !userStore.token ||
            now - lastTouchAt < SessionConfig.heartbeatThrottleMs
        ) {
            return;
        }
        lastTouchAt = now;
        touching = true;
        try {
            await touch({ silent: true });
        } catch {
        } finally {
            touching = false;
        }
    }

    onMounted(() => {
        ACTIVITY_EVENTS.forEach((event) => {
            document.addEventListener(event, onActivity, {
                capture: true,
                passive: true,
            });
        });
    });

    onUnmounted(() => {
        ACTIVITY_EVENTS.forEach((event) => {
            document.removeEventListener(event, onActivity, { capture: true });
        });
    });
}
