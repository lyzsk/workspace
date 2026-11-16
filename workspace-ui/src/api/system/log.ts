import { download, request } from "@/utils/http";
import { formatFileStamp } from "@/utils/format";
import type { LoginLogRow, OperLogRow, PageResult } from "@/types/system";

export function pageLoginLogs(params: Record<string, unknown>) {
    return request<PageResult<LoginLogRow>>({
        url: "/system/login-logs",
        method: "get",
        params,
    });
}

export function pageOperLogs(params: Record<string, unknown>) {
    return request<PageResult<OperLogRow>>({
        url: "/system/oper-logs",
        method: "get",
        params,
    });
}

export function exportLoginLogs(params: Record<string, unknown>) {
    return download(
        { url: "/system/login-logs/export", method: "get", params },
        `login-logs_${formatFileStamp()}.xlsx`,
    );
}

export function exportOperLogs(params: Record<string, unknown>) {
    return download(
        { url: "/system/oper-logs/export", method: "get", params },
        `oper-logs_${formatFileStamp()}.xlsx`,
    );
}
