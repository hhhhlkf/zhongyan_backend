package com.zhongyan.uav.common.error;

public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        this(ErrorCode.INTERNAL_ERROR, message);
    }

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.message());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.code();
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.code();
    }

    public int getCode() {
        return code;
    }
}

