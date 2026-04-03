package com.queue.application.common.exception;

public interface ErrorCode {

    int status();

    String code();

    String message();
}