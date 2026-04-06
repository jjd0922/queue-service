package com.queue.domain.exception;

import lombok.Getter;

@Getter
public class QueueException extends RuntimeException{

    private final QueueErrorCode errorCode;

    public QueueException(QueueErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

}
