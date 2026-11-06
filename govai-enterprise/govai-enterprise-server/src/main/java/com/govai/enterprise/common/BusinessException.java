package com.govai.enterprise.common;

/**
 * 业务异常:由全局异常处理器统一捕获并转为 Result.fail
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
