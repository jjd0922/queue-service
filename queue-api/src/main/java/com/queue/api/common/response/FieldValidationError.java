package com.queue.api.common.response;

public class FieldValidationError {

    private final String field;
    private final Object rejectedValue;
    private final String reason;

    private FieldValidationError(String field, Object rejectedValue, String reason) {
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.reason = reason;
    }

    public static FieldValidationError of(String field, Object rejectedValue, String reason) {
        return new FieldValidationError(field, rejectedValue, reason);
    }

    public String getField() {
        return field;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }

    public String getReason() {
        return reason;
    }
}