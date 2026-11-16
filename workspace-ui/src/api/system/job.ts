import { request } from "@/utils/http";
import type { PageResult } from "@/types/system";

export interface JobRow {
    id: string;
    jobName: string;
    jobGroup: string;
    cronExpression: string;
    handlerName: string;
    params?: string;
    misfirePolicy: number;
    status: number;
    createTime?: string;
}

export interface JobForm {
    jobName: string;
    jobGroup?: string;
    cronExpression: string;
    handlerName: string;
    params?: string;
    misfirePolicy?: number;
    status?: number;
}

export interface JobLogRow {
    id: string;
    jobId: string;
    jobName: string;
    handlerName: string;
    params?: string;
    status: number;
    errorMsg?: string;
    costTime?: number;
    startTime?: string;
    endTime?: string;
}

export function pageJobs(params: Record<string, unknown>) {
    return request<PageResult<JobRow>>({
        url: "/system/jobs",
        method: "get",
        params,
    });
}

export function createJob(data: JobForm) {
    return request<string>({ url: "/system/jobs", method: "post", data });
}

export function updateJob(id: string, data: JobForm) {
    return request<void>({ url: `/system/jobs/${id}`, method: "put", data });
}

export function deleteJob(id: string) {
    return request<void>({ url: `/system/jobs/${id}`, method: "delete" });
}

export function pauseJob(id: string) {
    return request<void>({ url: `/system/jobs/${id}/pause`, method: "put" });
}

export function resumeJob(id: string) {
    return request<void>({ url: `/system/jobs/${id}/resume`, method: "put" });
}

export function runJob(id: string) {
    return request<void>({ url: `/system/jobs/${id}/run`, method: "post" });
}

export interface CronValidation {
    valid: boolean;
    message: string;
    rule: string;
    nextTimes: string[];
}

export function validateCronExpression(expression: string) {
    return request<CronValidation>({
        url: "/system/jobs/cron/validate",
        method: "post",
        data: { expression },
    });
}

export function pageJobLogs(params: Record<string, unknown>) {
    return request<PageResult<JobLogRow>>({
        url: "/system/job-logs",
        method: "get",
        params,
    });
}
