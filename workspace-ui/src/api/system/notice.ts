import { request } from "@/utils/http";
import type { PageResult } from "@/types/system";

export interface NoticeRow {
    id: string;
    title: string;
    content: string;
    noticeType: string;
    priority: number;
    repeatIntervalSeconds: number;
    maxRemindCount: number;
    templateCode?: string;
    templateParams?: string;
    targetType: string;
    targetIds: string;
    status: number;
    publishTime?: string;
    remindTime?: string;
    expireTime?: string;
    createTime?: string;
}

export interface NoticeForm {
    title: string;
    content: string;
    noticeType: string;
    priority: number;
    repeatIntervalSeconds: number;
    maxRemindCount: number;
    templateCode?: string;
    templateParams?: Record<string, string>;
    targetType: string;
    targetIds: string[];
    remindTime?: string;
    expireTime?: string;
}

export interface NoticePublishForm {
    linkUrl?: string;
    dueTime?: string;
}

export function pageNotices(params: Record<string, unknown>) {
    return request<PageResult<NoticeRow>>({
        url: "/system/notices",
        method: "get",
        params,
    });
}

export function createNotice(data: NoticeForm) {
    return request<string>({ url: "/system/notices", method: "post", data });
}

export function publishNotice(id: string, data: NoticePublishForm) {
    return request<number>({
        url: `/system/notices/${id}/publish`,
        method: "put",
        data,
    });
}

export function uploadNoticeAttachment(id: string, file: File) {
    const data = new FormData();
    data.append("file", file);
    return request<{ id: string; originalName: string; sizeBytes: number }>({
        url: `/system/notices/${id}/attachments`,
        method: "post",
        data,
    });
}
