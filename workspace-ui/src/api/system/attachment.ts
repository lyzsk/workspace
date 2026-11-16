import { download, request } from "@/utils/http";

export interface NoticeAttachment {
    id: string;
    originalName: string;
    contentType?: string;
    sizeBytes: number;
}

export function listNoticeAttachments(noticeId: string) {
    return request<NoticeAttachment[]>({
        url: `/system/notices/${noticeId}/attachments`,
        method: "get",
    });
}

export function downloadNoticeAttachment(noticeId: string, fileId: string, filename: string) {
    return download({
        url: `/system/notices/${noticeId}/attachments/${fileId}`,
        method: "get",
    }, filename);
}
