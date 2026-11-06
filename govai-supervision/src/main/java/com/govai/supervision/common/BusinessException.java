package com.govai.supervision.common;

import lombok.Getter;

/**
 * 业务异常
 * 抛出时由 GlobalExceptionHandler 统一捕获并转换为 Result
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(String message) {
        this(500, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}