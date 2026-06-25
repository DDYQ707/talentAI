package com.talent.admin.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;


import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体
 * <p>
 * 结构：{ code: 200, message: "success", data: T }
 *
 * @author TalentAI
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码：200 成功 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 响应数据 */
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功（无数据） */
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    /** 成功（带数据） */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /** 成功（带数据和自定义消息） */
    public static <T> Result<T> success(T data, String message) {
        return new Result<>(200, message, data);
    }

    /** 失败（默认 500） */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    /** 失败（自定义状态码） */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}