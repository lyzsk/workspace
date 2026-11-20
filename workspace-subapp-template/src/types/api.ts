/** 后端统一返回结构 */
export interface ApiResult<T> {
    code: number;
    msg: string;
    data: T;
}

/** 分页返回结构 */
export interface PageResult<T> {
    rows: T[];
    total: number;
}

/** 分页查询参数 */
export interface PageQuery {
    pageNum: number;
    pageSize: number;
}

/** 业务状态码。与后端约定保持一致，新增状态码在这里补充 */
export const ApiStatus = {
    success: 200,
    unauthorized: 401,
    forbidden: 403,
} as const;
