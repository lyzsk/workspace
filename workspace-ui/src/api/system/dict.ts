import { download, request } from "@/utils/http";
import { formatFileStamp } from "@/utils/format";
import type { DictDataRow, DictTypeRow, PageResult } from "@/types/system";

export interface DictTypeForm {
    dictType: string;
    dictName: string;
    status?: number;
    remark?: string;
}

export interface DictDataForm {
    dictType: string;
    label: string;
    value: string;
    sortNo?: number;
    status?: number;
    remark?: string;
}

export function pageDictTypes(params: Record<string, unknown>) {
    return request<PageResult<DictTypeRow>>({
        url: "/system/dict-types",
        method: "get",
        params,
    });
}

export function listDictTypes() {
    return request<DictTypeRow[]>({
        url: "/system/dict-types/list",
        method: "get",
    });
}

export function createDictType(data: DictTypeForm) {
    return request<string>({ url: "/system/dict-types", method: "post", data });
}

export function updateDictType(id: string, data: DictTypeForm) {
    return request<void>({
        url: `/system/dict-types/${id}`,
        method: "put",
        data,
    });
}

export function deleteDictType(id: string) {
    return request<void>({
        url: `/system/dict-types/${id}`,
        method: "delete",
    });
}

export function exportDictTypes(params: Record<string, unknown>) {
    return download(
        { url: "/system/dict-types/export", method: "get", params },
        `dict-types_${formatFileStamp()}.xlsx`,
    );
}

export function exportDictData(params: Record<string, unknown>) {
    return download(
        { url: "/system/dict-data/export", method: "get", params },
        `dict-data_${formatFileStamp()}.xlsx`,
    );
}

export function pageDictData(params: Record<string, unknown>) {
    return request<PageResult<DictDataRow>>({
        url: "/system/dict-data",
        method: "get",
        params,
    });
}

export function listDictDataByType(dictType: string) {
    return request<DictDataRow[]>({
        url: `/system/dict-data/type/${dictType}`,
        method: "get",
    });
}

export function createDictData(data: DictDataForm) {
    return request<string>({ url: "/system/dict-data", method: "post", data });
}

export function updateDictData(id: string, data: DictDataForm) {
    return request<void>({
        url: `/system/dict-data/${id}`,
        method: "put",
        data,
    });
}

export function deleteDictData(id: string) {
    return request<void>({
        url: `/system/dict-data/${id}`,
        method: "delete",
    });
}
