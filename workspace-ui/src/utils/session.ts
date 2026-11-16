import { saveAllDrafts, markSessionEnding } from "@/utils/draft";
import { useUserStore } from "@/store/modules/user";

let expired = false;
let redirecting = false;

export function markExpired() {
    if (expired) {
        return;
    }
    expired = true;
    window.dispatchEvent(new Event("workspace:session-expired"));
}

export function isExpired() {
    return expired;
}

export function resetExpired() {
    expired = false;
    redirecting = false;
}

export function handleSessionExpired(reason: "expired" | "idle" = "expired") {
    if (redirecting) {
        return;
    }
    if (window.location.pathname === "/login") {
        return;
    }
    redirecting = true;
    markSessionEnding();
    saveAllDrafts();
    useUserStore().logout();
    const redirect =
        window.location.pathname + window.location.search + window.location.hash;
    const query = new URLSearchParams({ redirect, reason });
    window.location.href = `/login?${query.toString()}`;
}
