import DisableDevtool from "disable-devtool";
import { useUserStore } from "@/store/modules/user";

const BLOCKED_CTRL_SHIFT_KEYS = new Set(["I", "J", "C", "K", "M", "E", "P"]);
const AUTO_REDIRECT_AT_KEY = "workspace:devtools-auto-redirect-at";
const AUTO_COOLDOWN_MS = 15000;

export function installDevtoolsGuard() {
    let navigating = false;

    function isAdmin() {
        try {
            const user = useUserStore().user;
            return user?.username === "admin" || user?.roles?.includes("SUPER_ADMIN") === true;
        } catch {
            return false;
        }
    }

    function forceRefresh() {
        if (isAdmin() || navigating) {
            return;
        }
        navigating = true;
        const target = "/devtools-blocked";
        if (window.location.pathname === target) {
            window.location.reload();
        } else {
            window.location.replace(target);
        }
    }

    function triggerByDetection() {
        if (isAdmin() || navigating) {
            return;
        }
        const now = Date.now();
        let lastRedirectAt = 0;
        try {
            lastRedirectAt = Number(sessionStorage.getItem(AUTO_REDIRECT_AT_KEY) || 0);
        } catch {
            // storage unavailable
        }
        if (now - lastRedirectAt < AUTO_COOLDOWN_MS) {
            return;
        }
        try {
            sessionStorage.setItem(AUTO_REDIRECT_AT_KEY, String(now));
        } catch {
            // storage unavailable
        }
        forceRefresh();
    }

    function onKeydown(event: KeyboardEvent) {
        if (isAdmin()) {
            return;
        }
        const key = event.key.toUpperCase();
        const blocked =
            event.key === "F12" ||
            (event.ctrlKey && event.shiftKey && BLOCKED_CTRL_SHIFT_KEYS.has(key)) ||
            (event.metaKey && event.altKey && ["I", "J", "C", "U"].includes(key)) ||
            (event.ctrlKey && key === "U");
        if (!blocked) {
            return;
        }
        event.preventDefault();
        event.stopImmediatePropagation();
        forceRefresh();
    }

    function devtoolsProbablyOpen() {
        const widthGap = window.outerWidth - window.innerWidth;
        const heightGap = window.outerHeight - window.innerHeight;
        return widthGap > 220 || heightGap > 320;
    }

    function checkViewport() {
        if (!document.hidden && devtoolsProbablyOpen()) {
            triggerByDetection();
        }
    }

    function onResize() {
        window.setTimeout(checkViewport, 200);
    }

    try {
        DisableDevtool.isSuspend = false;
    } catch {
        // ignore
    }
    DisableDevtool({
        ignore: isAdmin,
        ondevtoolopen: triggerByDetection,
        detectors: "all",
        interval: 500,
        clearLog: true,
        disableMenu: false,
        clearIntervalWhenDevOpenTrigger: false,
    });

    document.addEventListener("keydown", onKeydown, true);
    window.addEventListener("resize", onResize);
    const viewportTimer = window.setInterval(checkViewport, 500);
    window.setTimeout(checkViewport, 0);

    return () => {
        document.removeEventListener("keydown", onKeydown, true);
        window.removeEventListener("resize", onResize);
        window.clearInterval(viewportTimer);
        try {
            DisableDevtool.isSuspend = true;
        } catch {
            // ignore
        }
    };
}
