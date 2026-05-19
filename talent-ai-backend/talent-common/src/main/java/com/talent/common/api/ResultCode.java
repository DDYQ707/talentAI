package com.talent.common.api;

/**
 * API 统一返回状态码枚举
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAILED(500, "系统异常，操作失败"),
    VALIDATE_FAILED(400, "参数检验失败"),
    UNAUTHORIZED(401, "暂未登录或 Token 已经过期"),
    FORBIDDEN(403, "没有相关权限，禁止访问"),
    NOT_FOUND(404, "请求的资源不存在");

    private final long code;
    private final String msg;

    ResultCode(long code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public long getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}