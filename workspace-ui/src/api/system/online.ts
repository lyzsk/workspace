import { request } from "@/utils/http";
import type { OnlineUserRow } from "@/types/system";

export function listOnlineUsers() {
    return request<OnlineUserRow[]>({
        url: "/system/online-users",
        method: "get",
    });
}

export function kickOnlineUser(userId: string) {
    return request<void>({
        url: `/system/online-users/${userId}`,
        method: "delete",
    });
}
