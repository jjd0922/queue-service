package com.queue.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QueueErrorCode implements ErrorCode {

    INVALID_QUEUE_ENTRY_STATE("QUEUE_001", "허용되지 않은 대기열 상태 전이입니다."),
    TERMINAL_QUEUE_ENTRY("QUEUE_002", "이미 종료된 대기열 엔트리입니다.");

    private final String code;
    private final String message;
}