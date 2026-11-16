import { download, request } from "@/utils/http";
import type { PageResult } from "@/types/system";

export type DocumentType = "FLOW" | "MIND" | "UML" | "WHITEBOARD";

export interface DocumentRow {
    id: string;
    title: string;
    docType: DocumentType;
    ownerId: string;
    content: string;
    revision: string;
    permission: "READ" | "WRITE";
    createTime: string;
    updateTime?: string;
}

export interface DocumentMember {
    id: string;
    userId: string;
    username: string;
    realName: string;
    permission: "READ" | "WRITE";
}

export interface DocumentOperation {
    opType: "UPSERT" | "DELETE" | "SCENE_REPLACE" | "PRESENCE";
    targetType?: "element" | "relation";
    targetId?: string;
    payload?: unknown;
}

export function pageDocuments(params: Record<string, unknown>) {
    return request<PageResult<DocumentRow>>({
        url: "/system/documents",
        method: "get",
        params,
    });
}

export function createDocument(data: { title: string; docType: DocumentType }) {
    return request<string>({ url: "/system/documents", method: "post", data });
}

export function getDocument(id: string) {
    return request<DocumentRow>({ url: `/system/documents/${id}`, method: "get" });
}

export function renameDocument(id: string, data: { title: string; docType: DocumentType }) {
    return request<void>({ url: `/system/documents/${id}`, method: "put", data });
}

export function deleteDocument(id: string) {
    return request<void>({ url: `/system/documents/${id}`, method: "delete" });
}

export function getDocumentMembers(id: string) {
    return request<DocumentMember[]>({
        url: `/system/documents/${id}/members`,
        method: "get",
    });
}

export function shareDocument(id: string, data: { userId: string; permission: string }) {
    return request<void>({
        url: `/system/documents/${id}/members`,
        method: "put",
        data,
    });
}

export function removeDocumentMember(id: string, userId: string) {
    return request<void>({
        url: `/system/documents/${id}/members/${userId}`,
        method: "delete",
    });
}

export function operateDocument(id: string, data: DocumentOperation) {
    return request<string>({
        url: `/system/documents/${id}/operations`,
        method: "post",
        data,
    });
}

export function importDocument(id: string, data: unknown) {
    return request<string>({
        url: `/system/documents/${id}/import`,
        method: "post",
        data,
    });
}

export function exportDocument(id: string, title: string) {
    return download({
        url: `/system/documents/${id}/export`,
        method: "get",
    }, `${title}.json`);
}
