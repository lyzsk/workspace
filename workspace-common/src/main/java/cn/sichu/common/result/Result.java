package cn.sichu.common.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Data
public class Result<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> failed() {
        return failed(ResultCode.FAILED);
    }

    public static <T> Result<T> failed(String msg) {
        return failed(ResultCode.FAILED, msg);
    }

    public static <T> Result<T> failed(IResultCode resultCode) {
        return failed(resultCode, resultCode.getMsg());
    }

    public static <T> Result<T> failed(IResultCode resultCode, String msg) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMsg(msg);
        return result;
    }

    public static <T> Result<T> judge(boolean success) {
        return success ? success() : failed();
    }

    public static boolean isSuccess(Result<?> result) {
        return result != null
            && ResultCode.SUCCESS.getCode() == result.getCode();
    }
}
