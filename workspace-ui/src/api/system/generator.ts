import { download, request } from "@/utils/http";

export interface GeneratorColumn {
    name: string;
    dataType: string;
    nullable: boolean;
    comment?: string;
}

export interface GeneratorRequest {
    schemaName: string;
    tableName: string;
    moduleName: string;
    businessName: string;
    packageName: string;
    functionName: string;
    permissionPrefix: string;
    className?: string;
    tenantScoped: boolean;
}

export function listGeneratorTables(schemaName: string) {
    return request<string[]>({
        url: "/generator/tables",
        method: "get",
        params: { schemaName },
    });
}

export function listGeneratorColumns(schemaName: string, tableName: string) {
    return request<GeneratorColumn[]>({
        url: "/generator/columns",
        method: "get",
        params: { schemaName, tableName },
    });
}

export function previewGenerator(data: GeneratorRequest) {
    return request<Record<string, string>>({
        url: "/generator/preview",
        method: "post",
        data,
    });
}

export function downloadGenerator(data: GeneratorRequest) {
    return download(
        { url: "/generator/download", method: "post", data },
        "workspace-generator.zip",
    );
}
