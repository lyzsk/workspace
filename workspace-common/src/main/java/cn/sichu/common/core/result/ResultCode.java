package cn.sichu.common.core.result;

import lombok.Getter;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Getter
public enum ResultCode implements IResultCode {

    SUCCESS(200, "操作成功"), FAILED(500, "操作失败"), PARAM_ERROR(400,
        "请求参数错误"), UNAUTHORIZED(401, "未登录或登录已过期"), FORBIDDEN(403,
        "没有权限"), NOT_FOUND(404, "资源不存在"), CONFLICT(409,
        "资源状态冲突"), BUSINESS_ERROR(400, "业务处理失败"), SERVICE_UNAVAILABLE(503,
        "服务暂不可用");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
