package com.azv.common;

import lombok.Data;

@Data
public class R<T> {
    private int code;        // 0=成功，非0=失败
    private String message;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 0;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> R<T> error(String message) {
        R<T> r = new R<>();
        r.code = 1;
        r.message = message;
        return r;
    }
}