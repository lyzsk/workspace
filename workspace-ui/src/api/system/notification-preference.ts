import { request } from "@/utils/http";

export interface NotificationPreference {
    quietEnabled: number;
    quietStart?: string;
    quietEnd?: string;
    timezone: string;
}

export function getNotificationPreference() {
    return request<NotificationPreference>({
        url: "/system/notification-preferences/my",
        method: "get",
    });
}

export function updateNotificationPreference(data: NotificationPreference) {
    return request<NotificationPreference>({
        url: "/system/notification-preferences/my",
        method: "put",
        data,
    });
}
