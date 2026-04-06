package com.queue.domain.exception;

public class QueueException extends RuntimeException{

    private final QueueErrorCode errorCode;

    public QueueException(QueueErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public QueueErrorCode getErrorCode() {
        return errorCode;
    }
}
