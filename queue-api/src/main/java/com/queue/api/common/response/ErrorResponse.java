package com.queue.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.queue.application.common.exception.ErrorCode;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String code;
    private final String message;
    private final String traceId;
    private final List<FieldValidationError> errors;

    private ErrorResponse(String code, String message, String traceId, List<FieldValidationError> errors) {
        this.code = code;
        this.message = message;
        this.traceId = traceId;
        this.errors = errors;
    }

    public static ErrorResponse of(ErrorCode errorCode, String traceId) {
        return new ErrorResponse(
                errorCode.code(),
                errorCode.message(),
                traceId,
                null
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String traceId, List<FieldValidationError> errors) {
        return new ErrorResponse(
                errorCode.code(),
                errorCode.message(),
                traceId,
                errors
        );
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getTraceId() {
        return traceId;
    }

    public List<FieldValidationError> getErrors() {
        return errors;
    }
}