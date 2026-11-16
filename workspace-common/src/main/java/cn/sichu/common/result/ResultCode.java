package cn.sichu.common.result;

import lombok.Getter;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Getter
public enum ResultCode implements IResultCode {

    SUCCESS("A0001", "操作成功"), FAILED("A0002", "操作失败"), PARAM_ERROR("A0003",
        "请求参数错误"), UNAUTHORIZED("A0004", "未登录或登录已过期"), FORBIDDEN("A0005",
        "没有权限"), NOT_FOUND("A0006", "资源不存在"), BUSINESS_ERROR("A0007", "业务处理失败");

    private final String code;
    private final String msg;

    ResultCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
