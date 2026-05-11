package com.zhongyan.uav.common.error;

import com.zhongyan.uav.common.response.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusinessException(BusinessException exception) {
        return ApiResult.failure(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? ErrorCode.BAD_REQUEST.message() : error.getDefaultMessage())
                .orElse(ErrorCode.BAD_REQUEST.message());
        return ApiResult.failure(ErrorCode.BAD_REQUEST.code(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleUnreadableBodyException(HttpMessageNotReadableException exception) {
        return ApiResult.failure(ErrorCode.BAD_REQUEST.code(), "request body is missing or invalid");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        return ApiResult.failure(ErrorCode.BAD_REQUEST.code(), exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ApiResult<Void> handleIllegalStateException(IllegalStateException exception) {
        return ApiResult.failure(ErrorCode.CONFLICT.code(), exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ApiResult<Void> handleNoSuchElementException(NoSuchElementException exception) {
        return ApiResult.failure(ErrorCode.NOT_FOUND.code(), exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResult<Void> handleAccessDeniedException(AccessDeniedException exception) {
        return ApiResult.failure(ErrorCode.FORBIDDEN.code(), ErrorCode.FORBIDDEN.message());
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception exception) {
        log.error("Unhandled exception", exception);
        return ApiResult.failure(ErrorCode.INTERNAL_ERROR.code(), ErrorCode.INTERNAL_ERROR.message());
    }
}

