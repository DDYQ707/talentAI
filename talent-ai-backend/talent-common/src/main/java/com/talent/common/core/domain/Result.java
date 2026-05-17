package com.talent.common.core.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 全局统一响应结果封装类
 */
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 状态码 (例如：200成功，500失败)
     */
    private Integer code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    public static <T> Result<T> success() {
        return restResult(null, 200, "操作成功");
    }

    public static <T> Result<T> success(T data) {
        return restResult(data, 200, "操作成功");
    }

    public static <T> Result<T> success(T data, String msg) {
        return restResult(data, 200, msg);
    }

    public static <T> Result<T> error(String msg) {
        return restResult(null, 500, msg);
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return restResult(null, code, msg);
    }

    private static <T> Result<T> restResult(T data, Integer code, String msg) {
        Result<T> apiResult = new Result<>();
        apiResult.setCode(code);
        apiResult.setData(data);
        apiResult.setMessage(msg);
        return apiResult;
    }
}