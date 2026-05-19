package com.talent.common.api;

import lombok.Data;
import java.io.Serializable;

/**
 * 通用返回对象
 */
@Data
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private long code;

    /**
     * 提示信息
     */
    private String msg;

    /**
     * 数据封装
     */
    private T data;

    protected R() {
    }

    protected R(long code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功返回结果
     */
    public static <T> R<T> ok() {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), null);
    }

    /**
     * 成功返回结果（带数据）
     *
     * @param data 获取的数据
     */
    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    /**
     * 成功返回结果（带数据和自定义消息）
     */
    public static <T> R<T> ok(T data, String msg) {
        return new R<>(ResultCode.SUCCESS.getCode(), msg, data);
    }

    /**
     * 失败返回结果
     */
    public static <T> R<T> fail() {
        return new R<>(ResultCode.FAILED.getCode(), ResultCode.FAILED.getMsg(), null);
    }

    /**
     * 失败返回结果（带自定义错误消息）
     *
     * @param msg 错误提示
     */
    public static <T> R<T> fail(String msg) {
        return new R<>(ResultCode.FAILED.getCode(), msg, null);
    }

    /**
     * 失败返回结果（使用指定错误码枚举）
     *
     * @param errorCode 错误码枚举
     */
    public static <T> R<T> fail(ResultCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMsg(), null);
    }
}