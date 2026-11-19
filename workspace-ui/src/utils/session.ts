import { useUserStore } from "@/store/modules/user";

export function isExpired() {
    return useUserStore().status === "expired";
}
