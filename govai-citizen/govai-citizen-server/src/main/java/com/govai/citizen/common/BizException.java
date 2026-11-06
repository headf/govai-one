package com.govai.citizen.common;

/**
 * 业务异常:由全局异常处理器转为 Result.fail
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(500, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
