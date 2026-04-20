package com.queue.domain.exception;

public class InvalidQueueEntryStateException extends DomainException {

    public InvalidQueueEntryStateException() {
        super(QueueErrorCode.INVALID_QUEUE_ENTRY_STATE);
    }
}