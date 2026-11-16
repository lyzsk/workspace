import { request } from "@/utils/http";
import type { PageResult } from "@/types/system";

export interface MessageRow {
    id: string;
    noticeId: string;
    title: string;
    content: string;
    category: string;
    level: number;
    linkUrl?: string;
    readFlag: number;
    readTime?: string;
    doneFlag: number;
    doneTime?: string;
    dueTime?: string;
    createTime: string;
}

export function pageMyMessages(params: Record<string, unknown>, silent = false) {
    return request<PageResult<MessageRow>>(
        {
            url: "/system/messages/my",
            method: "get",
            params,
        },
        { silent },
    );
}

export function getUnreadCount(silent = false) {
    return request<number>(
        {
            url: "/system/messages/unread-count",
            method: "get",
        },
        { silent },
    );
}

export function readMessage(id: string) {
    return request<void>({
        url: `/system/messages/${id}/read`,
        method: "put",
    });
}

export function readAllMessages() {
    return request<number>({
        url: "/system/messages/read-all",
        method: "put",
    });
}

export function doneMessage(id: string) {
    return request<void>({
        url: `/system/messages/${id}/done`,
        method: "put",
    });
}
