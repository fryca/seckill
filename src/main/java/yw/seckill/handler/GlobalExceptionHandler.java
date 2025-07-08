package yw.seckill.handler;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import yw.seckill.config.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception e) {
        // TODO: 记录日志
        return ApiResponse.error(e.getMessage());
    }
} 