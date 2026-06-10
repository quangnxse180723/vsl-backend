package com.vslbackend.exception;

import lombok.Getter;

/**
 * Exception nghiep vu chung cua he thong. Luon di kem mot {@link ErrorCode}
 * de {@link GlobalExceptionHandler} dich sang response JSON chuan.
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** Cho phep ghi de message mac dinh khi can chi tiet hon. */
    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
