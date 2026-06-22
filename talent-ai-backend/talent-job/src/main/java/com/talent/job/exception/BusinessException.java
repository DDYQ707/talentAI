package com.talent.job.exception;

import com.talent.common.api.ResultCode;

/**
 * 自定义业务异常
 */
public class BusinessException extends RuntimeException {

    private final long code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.FAILED.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }

    public BusinessException(long code, String message) {
        super(message);
        this.code = code;
    }

    public long getCode() {
        return code;
    }
}
