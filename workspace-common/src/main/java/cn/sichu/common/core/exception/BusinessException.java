package cn.sichu.common.core.exception;

import cn.sichu.common.core.result.IResultCode;
import cn.sichu.common.core.result.ResultCode;
import lombok.Getter;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Getter
public class BusinessException extends RuntimeException {
    private final IResultCode resultCode;

    public BusinessException(String message) {
        this(ResultCode.BUSINESS_ERROR, message);
    }

    public BusinessException(IResultCode resultCode) {
        this(resultCode, resultCode.getMsg());
    }

    public BusinessException(IResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
