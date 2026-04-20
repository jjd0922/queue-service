package com.queue.domain.exception;

public class TerminalQueueEntryException extends DomainException {

    public TerminalQueueEntryException() {
        super(QueueErrorCode.TERMINAL_QUEUE_ENTRY);
    }
}