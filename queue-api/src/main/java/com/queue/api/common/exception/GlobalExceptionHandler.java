package com.queue.api.common.exception;

import com.queue.api.common.response.ApiResponse;
import com.queue.api.common.response.ErrorResponse;
import com.queue.api.common.response.FieldValidationError;
import com.queue.api.common.trace.TraceIdFilter;
import com.queue.application.common.exception.BaseException;
import com.queue.application.common.exception.CommonErrorCode;
import com.queue.application.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("Business exception. traceId={}, code={}, message={}",
                traceId(request), errorCode.code(), errorCode.message());

        return ResponseEntity.status(errorCode.status())
                .body(ApiResponse.failure(ErrorResponse.of(errorCode, traceId(request))));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        List<FieldValidationError> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldValidationError)
                .toList();

        return ResponseEntity.status(CommonErrorCode.INVALID_INPUT.status())
                .body(ApiResponse.failure(ErrorResponse.of(CommonErrorCode.INVALID_INPUT, traceId(request), errors)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        List<FieldValidationError> errors = e.getConstraintViolations()
                .stream()
                .map(this::toFieldValidationError)
                .toList();

        return ResponseEntity.status(CommonErrorCode.INVALID_INPUT.status())
                .body(ApiResponse.failure(ErrorResponse.of(CommonErrorCode.INVALID_INPUT, traceId(request), errors)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(CommonErrorCode.INVALID_TYPE.status())
                .body(ApiResponse.failure(ErrorResponse.of(CommonErrorCode.INVALID_TYPE, traceId(request))));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(CommonErrorCode.INVALID_JSON.status())
                .body(ApiResponse.failure(ErrorResponse.of(CommonErrorCode.INVALID_JSON, traceId(request))));
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiResponse<Void>> handleErrorResponseException(
            ErrorResponseException e,
            HttpServletRequest request
    ) {
        CommonErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(e.getStatusCode())
                .body(ApiResponse.failure(ErrorResponse.of(errorCode, traceId(request))));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception. traceId={}", traceId(request), e);

        return ResponseEntity.status(CommonErrorCode.INTERNAL_SERVER_ERROR.status())
                .body(ApiResponse.failure(ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR, traceId(request))));
    }

    private FieldValidationError toFieldValidationError(FieldError error) {
        return FieldValidationError.of(
                error.getField(),
                error.getRejectedValue(),
                error.getDefaultMessage()
        );
    }

    private FieldValidationError toFieldValidationError(ConstraintViolation<?> violation) {
        return FieldValidationError.of(
                violation.getPropertyPath().toString(),
                violation.getInvalidValue(),
                violation.getMessage()
        );
    }

    private String traceId(HttpServletRequest request) {
        Object value = request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        return value != null ? value.toString() : null;
    }
}