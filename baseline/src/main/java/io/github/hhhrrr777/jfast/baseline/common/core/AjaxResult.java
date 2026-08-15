package io.github.hhhrrr777.jfast.baseline.common.core;

import java.io.Serializable;

/**
 * 统一响应体。code/msg/data 三段式,与前端 request.ts 的 ApiResult 契约对应。
 */
public class AjaxResult implements Serializable {

    public static final int SUCCESS = 200;
    public static final int ERROR = 500;

    private int code;
    private String msg;
    private Object data;

    public AjaxResult() {
    }

    public AjaxResult(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static AjaxResult success() {
        return new AjaxResult(SUCCESS, "操作成功", null);
    }

    public static AjaxResult success(Object data) {
        return new AjaxResult(SUCCESS, "操作成功", data);
    }

    public static AjaxResult error(String msg) {
        return new AjaxResult(ERROR, msg, null);
    }

    public static AjaxResult error(int code, String msg) {
        return new AjaxResult(code, msg, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
