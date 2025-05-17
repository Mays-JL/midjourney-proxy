package com.github.novicezk.midjourney.result;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
/*
ruoyi响应体
 */
@Data
@NoArgsConstructor
public class Result<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 成功
     */
    public static final int SUCCESS = 200;

    /**
     * 失败
     */
    public static final int FAIL = 500;

    private int code;

    private String msg;

    private T data;

    public static <T>  Result<T> ok() {
        return restResult(null, SUCCESS, "操作成功");
    }

    public static <T>  Result<T> ok(T data) {
        return restResult(data, SUCCESS, "操作成功");
    }

    public static <T>  Result<T> ok(String msg) {
        return restResult(null, SUCCESS, msg);
    }

    public static <T>  Result<T> ok(String msg, T data) {
        return restResult(data, SUCCESS, msg);
    }

    public static <T>  Result<T> fail() {
        return restResult(null, FAIL, "操作失败");
    }

    public static <T>  Result<T> fail(String msg) {
        return restResult(null, FAIL, msg);
    }

    public static <T>  Result<T> fail(T data) {
        return restResult(data, FAIL, "操作失败");
    }

    public static <T>  Result<T> fail(String msg, T data) {
        return restResult(data, FAIL, msg);
    }

    public static <T>  Result<T> fail(int code, String msg) {
        return restResult(null, code, msg);
    }

    /**
     * 返回警告消息
     *
     * @param msg 返回内容
     * @return 警告消息
     */
    public static <T>  Result<T> warn(String msg) {
        return restResult(null, HttpStatus.WARN, msg);
    }

    /**
     * 返回警告消息
     *
     * @param msg 返回内容
     * @param data 数据对象
     * @return 警告消息
     */
    public static <T>  Result<T> warn(String msg, T data) {
        return restResult(data, HttpStatus.WARN, msg);
    }

    private static <T>  Result<T> restResult(T data, int code, String msg) {
        Result<T> r = new  Result<>();
        r.setCode(code);
        r.setData(data);
        r.setMsg(msg);
        return r;
    }

    public static <T> Boolean isError( Result<T> ret) {
        return !isSuccess(ret);
    }

    public static <T> Boolean isSuccess( Result<T> ret) {
        return  Result.SUCCESS == ret.getCode();
    }
}
