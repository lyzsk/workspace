import { request } from "@/utils/http";

export type MetricValue = number | string;

export interface DailyVisit {
    day: string;
    pv: MetricValue;
    uv: MetricValue;
}

export interface DailyMetric {
    day: string;
    value: MetricValue;
}

export interface AppVisit {
    appCode: string;
    appName: string;
    visits: MetricValue;
}

export interface UserVisit {
    userId: string;
    username: string;
    realName?: string;
    departmentName: string;
    visits: MetricValue;
    lastVisitTime?: string;
}

export interface DepartmentValue {
    name: string;
    value: MetricValue;
}

export interface AppDepartment {
    appCode: string;
    departmentName: string;
    userCount: MetricValue;
}

export interface DashboardData {
    totalVisits: MetricValue;
    todayVisits: MetricValue;
    weekApiCalls: MetricValue;
    todayUv: MetricValue;
    onlineUsers: MetricValue;
    totalUsers: MetricValue;
    totalApps: MetricValue;
    enabledApps: MetricValue;
    visitsWeekChange: number;
    todayVisitsWeekChange: number;
    apiCallsWeekChange: number;
    activeUsersWeekChange: number;
    usersWeekChange: number;
    weekTrend: DailyVisit[];
    userGrowth: DailyMetric[];
    apiCallTrend: DailyMetric[];
    appRanking: AppVisit[];
    topUsers: UserVisit[];
    userDepartments: DepartmentValue[];
    appDepartments: AppDepartment[];
}

export function getDashboard() {
    return request<DashboardData>({ url: "/system/dashboard", method: "get" });
}

export function recordApplicationAccess(appCode: string) {
    return request<void>({
        url: `/system/dashboard/access-events/${appCode}`,
        method: "post",
    });
}

export function metric(value: MetricValue | undefined) {
    return Number(value ?? 0);
}
